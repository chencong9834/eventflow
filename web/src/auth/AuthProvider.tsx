import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { http } from "../shared/http";
import {
  clearSession,
  persistSession,
  readStoredToken,
  readStoredUser,
  setUnauthorizedHandler
} from "../shared/session";
import type { ApiResponse, CurrentUser, LoginPayload } from "../shared/types";

interface AuthContextValue {
  ready: boolean;
  token: string;
  user: CurrentUser | null;
  login: (username: string, password: string) => Promise<CurrentUser>;
  logout: () => void;
  refreshMe: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false);
  const [token, setToken] = useState(readStoredToken);
  const [user, setUser] = useState<CurrentUser | null>(readStoredUser);

  useEffect(() => {
    setUnauthorizedHandler(() => {
      setToken("");
      setUser(null);
    });
    return () => setUnauthorizedHandler(undefined);
  }, []);

  useEffect(() => {
    const existing = readStoredToken();
    if (!existing) {
      clearSession();
      setToken("");
      setUser(null);
      setReady(true);
      return;
    }
    let cancelled = false;
    http
      .get("/auth/me")
      .then((payload) => {
        if (cancelled) {
          return;
        }
        const res = payload as unknown as ApiResponse<CurrentUser>;
        persistSession(existing, res.data);
        setToken(existing);
        setUser(res.data);
      })
      .catch(() => {
        if (cancelled) {
          return;
        }
        clearSession();
        setToken("");
        setUser(null);
      })
      .finally(() => {
        if (!cancelled) {
          setReady(true);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      ready,
      token,
      user,
      login: async (username, password) => {
        const res = (await http.post("/auth/login", { username, password })) as ApiResponse<LoginPayload>;
        persistSession(res.data.token, res.data.user);
        setToken(res.data.token);
        setUser(res.data.user);
        return res.data.user;
      },
      logout: () => {
        clearSession();
        setToken("");
        setUser(null);
      },
      refreshMe: async () => {
        const res = (await http.get("/auth/me")) as ApiResponse<CurrentUser>;
        const existing = readStoredToken();
        if (existing) {
          persistSession(existing, res.data);
        }
        setUser(res.data);
      }
    }),
    [ready, token, user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return ctx;
}

export function homeByTenantType(tenantType?: string) {
  if (tenantType === "PLATFORM") return "/platform/home";
  if (tenantType === "ORGANIZER") return "/organizer/home";
  if (tenantType === "BUYER") return "/buyer/home";
  return "/login";
}
