// =========================================
// API 유틸리티: 토큰 메모리 관리 + 자동 재발급
// - accessToken: localStorage 제거 → JS 메모리 변수 (XSS 방어)
// - refreshToken: httpOnly 쿠키 (서버에서 Set-Cookie, JS 접근 불가)
// =========================================
import { showToast } from './toast.js';

export const API_BASE = '/api';

let _accessToken = null;
// 동시 401 발생 시 재발급 요청 중복 방지
let _reissuePromise = null;

export function setAccessToken(token) { _accessToken = token; }
export function clearAccessToken()    { _accessToken = null; }
export function getAccessToken()      { return _accessToken; }

export function getAuthHeaders() {
    return _accessToken ? { 'Authorization': 'Bearer ' + _accessToken } : {};
}

/**
 * 재발급 요청 단일화: 동시에 여러 401이 오더라도 서버에 한 번만 요청
 */
async function reissueToken() {
    if (_reissuePromise) return _reissuePromise;
    _reissuePromise = (async () => {
        try {
            const res = await fetch(`${API_BASE}/users/reissue`, { method: 'POST' });
            if (res.ok) {
                const data = await res.json();
                setAccessToken(data.accessToken);
                return data.accessToken;
            }
            return null;
        } finally {
            _reissuePromise = null;
        }
    })();
    return _reissuePromise;
}

/**
 * fetchWithAuth: 401 시 쿠키의 refreshToken으로 자동 재발급 후 재시도
 */
export async function fetchWithAuth(url, options = {}) {
    if (!options.headers) options.headers = {};
    if (_accessToken) options.headers['Authorization'] = 'Bearer ' + _accessToken;

    let response = await fetch(url, options);

    if (response.status === 401) {
        try {
            const newToken = await reissueToken();
            if (newToken) {
                options.headers['Authorization'] = 'Bearer ' + newToken;
                response = await fetch(url, options);
            } else {
                showToast('세션이 만료되었습니다. 다시 로그인해 주세요.', 'error');
                clearAccessToken();
                localStorage.removeItem('username');
                window.dispatchEvent(new CustomEvent('auth:expired'));
            }
        } catch (error) {
            console.error('토큰 재발급 오류:', error);
            clearAccessToken();
            window.dispatchEvent(new CustomEvent('auth:expired'));
        }
    }
    return response;
}
