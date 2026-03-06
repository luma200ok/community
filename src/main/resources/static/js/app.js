const API_BASE = "/api";
let currentPostId = null;

// ==========================================
// 💡 0. 토큰 안전하게 가져오기 (JS 에러 방어용!)
// ==========================================
function getAuthHeaders() {
    const token = localStorage.getItem("accessToken");
    // 로컬 스토리지에 글자 그대로 "null"이나 "undefined"가 들어간 경우를 완벽 차단!
    if (token && token !== "null" && token !== "undefined" && token.trim() !== "") {
        return { "Authorization": "Bearer " + token };
    }
    return {};
}

// ==========================================
// 💡 1. JWT 파싱 및 상단 헤더 UI 렌더링
// ==========================================
function parseJwt(token) {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join(''));
        return JSON.parse(jsonPayload);
    } catch (e) { return null; }
}

function renderHeader() {
    const headers = getAuthHeaders();
    const authSection = document.getElementById("auth-section");

    if (headers.Authorization) {
        // ⭐ 복잡하게 토큰 뜯을 필요 없이, 아까 저장한 아이디를 꺼내옵니다!
        const username = localStorage.getItem("username") || "회원";

        authSection.innerHTML = `
            <span style="font-size: 14px; color: #333; margin-right: 10px;"><b>${username}</b>님 환영합니다!</span>
            <button class="btn btn-primary" onclick="toggleForm('write')">✏️ 글쓰기</button>
            <button class="btn btn-text" onclick="logout()">로그아웃</button>
        `;
    } else {
        authSection.innerHTML = `
            <button class="btn btn-text" onclick="toggleForm('login')">로그인</button>
            <button class="btn btn-primary" onclick="toggleForm('signup')">회원가입</button>
        `;
    }
}

// ==========================================
// 💡 2. 화면 섹션 전환 로직 (토글)
// ==========================================
function hideAllForms() {
    ['login-section', 'signup-section', 'write-section'].forEach(id => {
        document.getElementById(id).style.display = 'none';
    });
}

function toggleForm(type) {
    hideAllForms();

    // ⭐ 화면이 전환되는 느낌을 위해 밑에 깔려있던 리스트와 상세페이지를 완전히 숨깁니다!
    document.getElementById('list-section').style.display = 'none';
    document.getElementById('detail-section').style.display = 'none';

    if (type === 'signup') document.getElementById('signup-section').style.display = 'block';
    if (type === 'login') document.getElementById('login-section').style.display = 'block';
    if (type === 'write') {
        const headers = getAuthHeaders();
        if(!headers.Authorization) {
            showList(); // 로그인 안 했으면 튕겨내고 다시 목록 보여주기
            return alert("로그인이 필요합니다!");
        }
        document.getElementById('write-section').style.display = 'block';
    }
}

// ==========================================
// 💡 3. 기존 인증 로직 (회원가입, 로그인, 로그아웃)
// ==========================================
async function signup() {
    const username = document.getElementById("reg-username").value;
    const password = document.getElementById("reg-password").value;
    const email = document.getElementById("reg-email").value;
    try {
        const response = await fetch(`${API_BASE}/users/signup`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password, email })
        });
        if (response.ok) {
            alert("🎉 회원가입 성공! 이제 로그인해주세요.");
            toggleForm('login');
        } else {
            const errorData = await response.json();
            alert("가입 실패: " + errorData.message);
        }
    } catch (error) { console.error(error); }
}

async function login() {
    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;
    try {
        const response = await fetch(`${API_BASE}/users/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password })
        });
        if (response.ok) {
            const data = await response.json();
            localStorage.setItem("accessToken", data.accessToken);
            // ⭐ 이 줄을 추가하세요! 로그인 성공 시 입력한 아이디를 기억해둡니다.
            localStorage.setItem("username", username);

            alert("로그인 성공!");
            renderHeader(); // 1. 헤더를 먼저 'ㅇㅇ님 환영합니다'로 바꾸고
            showList();     // 2. 숨겨놨던 게시글 목록을 짠! 하고 다시 보여줍니다.
        } else {
            const errorData = await response.json();
            alert("로그인 실패: " + errorData.message);
        }
    } catch (error) { console.error(error); }
}

async function logout() {
    const headers = getAuthHeaders();
    if (headers.Authorization) {
        await fetch(`${API_BASE}/users/logout`, { method: "POST", headers: headers });
    }
    localStorage.removeItem("accessToken");
    alert("로그아웃 되었습니다.");
    location.reload();
}

// ==========================================
// 💡 4. 게시글 로직 (카드 형태로 그리기 적용!)
// ==========================================
async function writePost() {
    const title = document.getElementById("title").value;
    const content = document.getElementById("content").value;
    const imageInput = document.getElementById("imageFile");
    const headers = getAuthHeaders();

    if(!headers.Authorization) return alert("로그인이 필요합니다!");

    const formData = new FormData();
    formData.append("request", new Blob([JSON.stringify({ title, content })], { type: "application/json" }));
    if (imageInput.files.length > 0) formData.append("images", imageInput.files[0]);

    try {
        const response = await fetch(`${API_BASE}/posts`, {
            method: "POST",
            headers: headers,
            body: formData
        });
        if (response.ok) {
            alert("게시글 등록 완료!");
            document.getElementById("title").value = "";
            document.getElementById("content").value = "";
            imageInput.value = "";
            hideAllForms();
            fetchPosts();
        } else { alert("작성 실패"); }
    } catch (error) { console.error(error); }
}

async function fetchPosts() {
    try {
        const keyword = document.getElementById("search-keyword").value;
        let url = keyword.trim() !== "" ? `${API_BASE}/posts?keyword=${encodeURIComponent(keyword)}` : `${API_BASE}/posts`;

        const response = await fetch(url);
        const data = await response.json();
        const posts = data.content;

        const postListDiv = document.getElementById("post-list");
        postListDiv.innerHTML = "";

        if (posts.length === 0) {
            postListDiv.innerHTML = "<p style='grid-column: 1/-1; text-align:center; color:#777;'>게시글이 없습니다.</p>";
            return;
        }

        posts.forEach(post => {
            const thumbImg = post.thumbnailUrl
                ? post.thumbnailUrl
                : "https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=300&q=80";

            const postHtml = `
                <div class="card" onclick="viewPost(${post.id})">
                    <img src="${thumbImg}" class="card-image" alt="썸네일">
                    <div class="card-content">
                        <span class="card-badge">자유</span>
                        <h3 class="card-title">${post.title}</h3>
                        <p class="card-summary">내용을 보려면 클릭하세요...</p>
                        <div class="card-footer">
                            <span>👤 ${post.writer}</span>
                            <span>👀 ${post.viewCount} | ❤️ ${post.likeCount}</span>
                        </div>
                    </div>
                </div>
            `;
            postListDiv.innerHTML += postHtml;
        });
    } catch (error) { console.error(error); }
}

// ==========================================
// 💡 5. 상세/수정/댓글 기존 로직 유지
// ==========================================
async function viewPost(postId) {
    currentPostId = postId;
    const headers = getAuthHeaders(); // 💡 여기서 안전하게 헤더를 세팅합니다!

    try {
        const response = await fetch(`${API_BASE}/posts/${postId}`, { headers });
        if (!response.ok) return alert("게시글을 불러올 수 없습니다.");
        const post = await response.json();

        hideAllForms();
        document.getElementById("list-section").style.display = "none";
        document.getElementById("detail-section").style.display = "block";

        document.getElementById("detail-title").innerText = post.title;
        document.getElementById("detail-meta").innerText = `👤 작성자: ${post.writer} | 👀 조회: ${post.viewCount} | 📅 ${post.createdAt}`;
        document.getElementById("detail-content").innerText = post.content;

        const imgDiv = document.getElementById("detail-images");
        imgDiv.innerHTML = "";
        post.imageUrls.forEach(url => {
            imgDiv.innerHTML += `<img src="${url}" style="max-width: 100%; border-radius: 8px; margin-bottom: 15px;">`;
        });

        const likeBtn = document.getElementById("like-btn");
        if (post.isLiked) {
            likeBtn.innerText = `❤️ 좋아요 취소 (${post.likeCount})`;
            likeBtn.style.backgroundColor = "#ff4d4d";
        } else {
            likeBtn.innerText = `🤍 좋아요 (${post.likeCount})`;
            likeBtn.style.backgroundColor = "#6c757d";
        }

        const commentDiv = document.getElementById("comment-list");
        commentDiv.innerHTML = "";
        if (post.comments.length === 0) commentDiv.innerHTML = "<p style='color:#777;'>첫 번째 댓글을 남겨보세요!</p>";

        post.comments.forEach(comment => {
            commentDiv.innerHTML += `
                <div class="comment-box">
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <div><b>${comment.writer}</b> <span style="font-size:0.8em; color:#999;">(${comment.createdAt})</span></div>
                        <button class="btn btn-outline" style="padding: 2px 8px; font-size: 12px; color: #dc3545; border-color: #dc3545;" onclick="deleteComment(${comment.id})">삭제</button>
                    </div>
                    <div style="margin-top:5px;">${comment.content}</div>
                </div>
            `;
        });
    } catch (error) { console.error(error); }
}

function showList() {
    hideAllForms(); // 열려있던 글쓰기, 로그인 폼 다 숨기고
    document.getElementById("detail-section").style.display = "none";
    document.getElementById("edit-section").style.display = "none";

    // ⭐ 리스트 섹션만 짠! 하고 다시 보여줍니다.
    document.getElementById("list-section").style.display = "block";
    fetchPosts();
}

async function toggleLike() {
    const headers = getAuthHeaders();
    if (!headers.Authorization) return alert("로그인이 필요합니다!");
    try {
        await fetch(`${API_BASE}/posts/${currentPostId}/likes`, { method: "POST", headers: headers });
        viewPost(currentPostId);
    } catch (error) { console.error(error); }
}

async function writeComment() {
    const content = document.getElementById("comment-input").value;
    const headers = getAuthHeaders();
    if (!headers.Authorization) return alert("로그인이 필요합니다!");
    if (!content.trim()) return alert("댓글 내용을 입력하세요.");

    headers["Content-Type"] = "application/json";

    try {
        const response = await fetch(`${API_BASE}/posts/${currentPostId}/comments`, {
            method: "POST", headers: headers,
            body: JSON.stringify({ content })
        });
        if (response.ok) { document.getElementById("comment-input").value = ""; viewPost(currentPostId); }
        else { alert("댓글 작성 실패!"); }
    } catch (error) { console.error(error); }
}

async function deleteComment(commentId) {
    if (!confirm("이 댓글을 삭제하시겠습니까?")) return;
    const headers = getAuthHeaders();
    if (!headers.Authorization) return alert("로그인이 필요합니다!");
    try {
        const response = await fetch(`${API_BASE}/comments/${commentId}`, { method: "DELETE", headers: headers });
        if (response.ok) viewPost(currentPostId);
        else alert("권한이 없습니다!");
    } catch (error) { console.error(error); }
}

async function deletePost() {
    if (!confirm("정말 이 게시글을 삭제하시겠습니까?")) return;
    const headers = getAuthHeaders();
    if (!headers.Authorization) return alert("로그인이 필요합니다!");
    try {
        const response = await fetch(`${API_BASE}/posts/${currentPostId}`, { method: "DELETE", headers: headers });
        if (response.ok) { alert("게시글이 삭제되었습니다."); showList(); }
        else { alert("권한이 없습니다!"); }
    } catch (error) { console.error(error); }
}

function showEditForm() {
    document.getElementById("detail-section").style.display = "none";
    document.getElementById("edit-section").style.display = "block";
    document.getElementById("edit-title").value = document.getElementById("detail-title").innerText;
    document.getElementById("edit-content").value = document.getElementById("detail-content").innerText;
}

function cancelEdit() {
    document.getElementById("edit-section").style.display = "none";
    document.getElementById("detail-section").style.display = "block";
}

async function updatePost() {
    const title = document.getElementById("edit-title").value;
    const content = document.getElementById("edit-content").value;
    const imageInput = document.getElementById("edit-imageFile");
    const headers = getAuthHeaders();
    if (!headers.Authorization) return alert("로그인이 필요합니다!");

    const formData = new FormData();
    formData.append("request", new Blob([JSON.stringify({ title, content })], { type: "application/json" }));
    if (imageInput.files.length > 0) formData.append("images", imageInput.files[0]);

    try {
        const response = await fetch(`${API_BASE}/posts/${currentPostId}`, { method: "PUT", headers: headers, body: formData });
        if (response.ok) { alert("수정 성공!"); document.getElementById("edit-section").style.display = "none"; viewPost(currentPostId); }
        else { alert("권한이 없습니다!"); }
    } catch (error) { console.error(error); }
}

// 💡 화면 켜지자마자 실행되는 곳
window.onload = function() {
    hideAllForms();
    renderHeader();
    fetchPosts();
};