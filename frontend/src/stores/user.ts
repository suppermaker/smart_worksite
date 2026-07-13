import { defineStore } from 'pinia';
import * as authApi from '../api/auth';
import type { UserInfo } from '../api/types';

const tokenKey = 'smart_worksite_token';
const userKey = 'smart_worksite_user';
const projectKey = 'smart_worksite_project';

const permissionAliases: Record<string, string[]> = {
  'file:view': ['file:manage'],
  'template:view': ['file:manage'],
  'audit:view': ['system:manage'],
  'datasource:view': ['datasource:manage']
};

function readStoredUser() {
  const raw = localStorage.getItem(userKey);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as UserInfo;
  } catch (error) {
    console.error('Stored user state is corrupted; clearing local auth state.', error);
    localStorage.removeItem(userKey);
    localStorage.removeItem(tokenKey);
    return null;
  }
}

const storedUser = readStoredUser();

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(tokenKey) || '',
    user: storedUser as UserInfo | null,
    permissions: storedUser?.permissions || [] as string[],
    roles: storedUser?.roles || [] as string[],
    loading: false,
    error: ''
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    displayName: (state) => state.user?.realName || state.user?.username || '未登录用户'
  },
  actions: {
    persistUser(user: UserInfo | null) {
      this.user = user;
      this.permissions = user?.permissions || [];
      this.roles = user?.roles || [];
      if (user) {
        localStorage.setItem(userKey, JSON.stringify(user));
        if (user.defaultProjectId) localStorage.setItem(projectKey, String(user.defaultProjectId));
      } else {
        localStorage.removeItem(userKey);
      }
    },
    setToken(token: string) {
      this.token = token;
      localStorage.setItem(tokenKey, token);
    },
    async login(payload: { username: string; password: string }) {
      this.loading = true;
      this.error = '';
      try {
        const result = await authApi.login(payload);
        this.setToken(result.accessToken);
        this.persistUser(result.user);
      } catch (error) {
        this.error = error instanceof Error ? error.message : '登录失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async fetchCurrentUser() {
      if (!this.token) return null;
      this.loading = true;
      this.error = '';
      try {
        const user = await authApi.fetchCurrentUser();
        this.persistUser(user);
        return user;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '获取当前用户失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async logout() {
      await authApi.logout().catch((error) => {
        console.warn('Logout API failed, clearing local auth state anyway.', error);
      });
      this.clearAuthState();
    },
    clearAuthState() {
      this.token = '';
      this.error = '';
      this.loading = false;
      this.persistUser(null);
      localStorage.removeItem(tokenKey);
    },
    hasPermission(permission?: string) {
      if (!permission) return true;
      if (this.roles.includes('PLATFORM_ADMIN')) return true;
      if (this.permissions.includes(permission)) return true;
      return (permissionAliases[permission] || []).some((alias) => this.permissions.includes(alias));
    }
  }
});
