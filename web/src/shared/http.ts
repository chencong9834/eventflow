import axios from "axios";
import type { ApiResponse } from "./types";
import { AUTH_TOKEN_KEY, notifyUnauthorized } from "./session";

export class ApiError extends Error {
  code: string;
  status?: number;
  constructor(code: string, message: string, status?: number) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

export const http = axios.create({
  baseURL: "/api"
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const status = err.response?.status as number | undefined;
    const url = String(err.config?.url ?? "");
    const isLogin = url.includes("/auth/login");
    if (status === 401 && !isLogin) {
      notifyUnauthorized();
    }
    const payload = err.response?.data as ApiResponse<unknown> | undefined;
    return Promise.reject(new ApiError(payload?.code ?? "NETWORK", payload?.message ?? err.message, status));
  }
);
