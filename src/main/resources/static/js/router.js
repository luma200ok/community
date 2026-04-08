// =========================================
// Hash 기반 라우터 (브라우저 뒤로가기 지원)
// =========================================
export function navigate(path) {
    window.location.hash = '#/' + path;
}

export function getRoute() {
    return window.location.hash.replace(/^#\/?/, '');
}

export function parsePostId(route) {
    const match = route.match(/^posts\/(\d+)$/);
    return match ? parseInt(match[1], 10) : null;
}
