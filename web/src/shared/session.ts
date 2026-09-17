import type { CurrentUser } from "./types";

export const AUTH_TOKEN_KEY = "eventflow.token";
export const AUTH_USER_KEY = "eventflow.user";

export function readStoredToken(): string {
  return localStorage.getItem(AUTH_TOKEN_KEY) ?? "";
}

export function readStoredUser(): CurrentUser | null {
  const raw = localStorage.getItem(AUTH_USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as CurrentUser;
  } catch {
    return null;
  }
}

export function persistSession(token: string, user: CurrentUser) {
  localStorage.setItem(AUTH_TOKEN_KEY, token);
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
}

export function clearSession() {
  localStorage.removeItem(AUTH_TOKEN_KEY);
  localStorage.removeItem(AUTH_USER_KEY);
}

let unauthorizedHandler: (() => void) | undefined;

export function setUnauthorizedHandler(handler: (() => void) | undefined) {
  unauthorizedHandler = handler;
}

export function notifyUnauthorized() {
  clearSession();
  unauthorizedHandler?.();
  if (!window.location.pathname.startsWith("/login")) {
    window.location.assign("/login");
  }
}
