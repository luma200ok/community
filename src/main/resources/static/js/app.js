const API_BASE = "/api";
let currentPostId = null;

// 1. 로그인/회원가입 폼 전환
function toggleForm(type) {
    if (type === 'signup') {
        document.getElementById('login-section').style.display = 'none';
        document.getElementById('signup-section').style.display = 'block';
    } else {
        document.getElementById('signup-section').style.display = 'none';
        document.getElementById('login-section').style.display = 'block';
    }
}

// 2. 회원가입
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
            document.getElementById("reg-username").value = "";
            document.getElementById("reg-password").value = "";
            document.getElementById("reg-email").value = "";
            toggleForm('login');
        } else {
            const errorData = await response.json();
            alert("가입 실패: " + errorData.message);
        }
    } catch (error) { console.error(error); }
}

// 3. 로그인
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
            alert("로그인 성공!");
            document.getElementById("login-section").style.display = "none";
            document.getElementById("logout-section").style.display = "block";
        } else {
            const errorData = await response.json();
            alert("로그인 실패: " + errorData.message);
        }
    } catch (error) { console.error(error); }
}

// 4. 로그아웃
async function logout() {
    const token = localStorage.getItem("accessToken");
    if (token) {
        await fetch(`${API_BASE}/users/logout`, {
            method: "POST",
            headers: { "Authorization": "Bearer " + token }
        });
    }
    localStorage.removeItem("accessToken");
    alert("로그아웃 되었습니다.");
    location.reload();
}

// 5. 게시글 작성
async function writePost() {
    const title = document.getElementById("title").value;
    const content = document.getElementById("content").value;
    const imageInput = document.getElementById("imageFile");
    const token = localStorage.getItem("accessToken");

    if (!token) return alert("로그인이 필요합니다!");

    const formData = new FormData();
    formData.append("request", new Blob([JSON.stringify({ title, content })], { type: "application/json" }));
    if (imageInput.files.length > 0) formData.append("images", imageInput.files[0]);

    try {
        const response = await fetch(`${API_BASE}/posts`, {
            method: "POST",
            headers: { "Authorization": "Bearer " + token },
            body: formData
        });
        if (response.ok) {
            alert("게시글 등록 완료!");
            document.getElementById("title").value = "";
            document.getElementById("content").value = "";
            imageInput.value = "";
            fetchPosts();
        } else {
            alert("작성 실패");
        }
    } catch (error) { console.error(error); }
}

// 6. 게시글 목록 불러오기 (💡 검색 기능 적용 완료!)
async function fetchPosts() {
    try {
        const keyword = document.getElementById("search-keyword") ? document.getElementById("search-keyword").value : "";
        let url = `${API_BASE}/posts`;

        // 검색어가 있으면 파라미터로 붙여서 요청!
        if (keyword.trim() !== "") {
            url += `?keyword=${encodeURIComponent(keyword)}`;
        }

        const response = await fetch(url);
        const data = await response.json();
        const posts = data.content;

        const postListDiv = document.getElementById("post-list");
        postListDiv.innerHTML = "";

        if (posts.length === 0) {
            postListDiv.innerHTML = "<p style='text-align:center; color:#777;'>게시글이 없습니다.</p>";
            return;
        }

        posts.forEach(post => {
            const postHtml = `
                <div class="post-card" onclick="viewPost(${post.id})">
                    <div class="post-title">${post.id}. ${post.title}</div>
                    <div class="post-meta">
                        작성자: ${post.writer} | 조회수: ${post.viewCount} | 좋아요: ${post.likeCount} | 작성일: ${post.createdAt}
                    </div>
                </div>
            `;
            postListDiv.innerHTML += postHtml;
        });
    } catch (error) { console.error(error); }
}

// 7. 게시글 상세 조회
async function viewPost(postId) {
    currentPostId = postId;
    const token = localStorage.getItem("accessToken");
    const headers = {};
    if (token) headers["Authorization"] = "Bearer " + token;

    try {
        const response = await fetch(`${API_BASE}/posts/${postId}`, { headers });
        if (!response.ok) return alert("게시글을 불러올 수 없습니다.");

        const post = await response.json();

        document.getElementById("top-section").style.display = "none";
        document.getElementById("list-section").style.display = "none";
        document.getElementById("edit-section").style.display = "none";
        document.getElementById("detail-section").style.display = "block";

        document.getElementById("detail-title").innerText = post.title;
        document.getElementById("detail-meta").innerText = `작성자: ${post.writer} | 조회수: ${post.viewCount} | 작성일: ${post.createdAt}`;
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
            // 💡 [NEW] 댓글 옆에 작고 귀여운 삭제 버튼 추가!
            commentDiv.innerHTML += `
                <div class="comment-box">
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <b>${comment.writer}</b> <span style="font-size:0.8em; color:#999;">(${comment.createdAt})</span>
                        </div>
                        <button onclick="deleteComment(${comment.id})" style="background-color: #dc3545; color: white; border: none; padding: 2px 8px; border-radius: 4px; font-size: 0.8em; width: auto;">삭제</button>
                    </div>
                    <div style="margin-top:5px;">${comment.content}</div>
                </div>
            `;
        });
    } catch (error) { console.error(error); }
}

// 8. 목록으로 돌아가기
function showList() {
    document.getElementById("top-section").style.display = "block";
    document.getElementById("list-section").style.display = "block";
    document.getElementById("detail-section").style.display = "none";
    document.getElementById("edit-section").style.display = "none";
    fetchPosts();
}

// 9. 좋아요 토글
async function toggleLike() {
    const token = localStorage.getItem("accessToken");
    if (!token) return alert("로그인이 필요합니다!");

    try {
        await fetch(`${API_BASE}/posts/${currentPostId}/likes`, {
            method: "POST",
            headers: { "Authorization": "Bearer " + token }
        });
        viewPost(currentPostId);
    } catch (error) { console.error(error); }
}

// 10. 댓글 작성
async function writeComment() {
    const content = document.getElementById("comment-input").value;
    const token = localStorage.getItem("accessToken");

    if (!token) return alert("로그인이 필요합니다!");
    if (!content.trim()) return alert("댓글 내용을 입력하세요.");

    try {
        const response = await fetch(`${API_BASE}/posts/${currentPostId}/comments`, {
            method: "POST",
            headers: {
                "Authorization": "Bearer " + token,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ content })
        });

        if (response.ok) {
            document.getElementById("comment-input").value = "";
            viewPost(currentPostId);
        } else {
            alert("댓글 작성 실패!");
        }
    } catch (error) { console.error(error); }
}

// 11. 💡 [NEW] 댓글 삭제 기능
async function deleteComment(commentId) {
    if (!confirm("이 댓글을 삭제하시겠습니까?")) return;

    const token = localStorage.getItem("accessToken");
    if (!token) return alert("로그인이 필요합니다!");

    try {
        // 백엔드 댓글 삭제 API 호출 (일반적인 REST 규약에 따름)
        const response = await fetch(`${API_BASE}/comments/${commentId}`, {
            method: "DELETE",
            headers: { "Authorization": "Bearer " + token }
        });

        if (response.ok) {
            viewPost(currentPostId); // 삭제 후 댓글창 새로고침!
        } else {
            alert("권한이 없습니다! (본인이 작성한 댓글만 삭제 가능합니다)");
        }
    } catch (error) { console.error(error); }
}

// 12. 게시글 삭제
async function deletePost() {
    if (!confirm("정말 이 게시글을 삭제하시겠습니까?")) return;

    const token = localStorage.getItem("accessToken");
    if (!token) return alert("로그인이 필요합니다!");

    try {
        const response = await fetch(`${API_BASE}/posts/${currentPostId}`, {
            method: "DELETE",
            headers: { "Authorization": "Bearer " + token }
        });

        if (response.ok) {
            alert("게시글이 삭제되었습니다.");
            showList();
        } else {
            alert("권한이 없습니다! (본인이 작성한 글만 삭제 가능합니다)");
        }
    } catch (error) { console.error(error); }
}

// 13. 수정 폼 열기
function showEditForm() {
    document.getElementById("detail-section").style.display = "none";
    document.getElementById("edit-section").style.display = "block";

    document.getElementById("edit-title").value = document.getElementById("detail-title").innerText;
    document.getElementById("edit-content").value = document.getElementById("detail-content").innerText;
}

// 14. 수정 취소
function cancelEdit() {
    document.getElementById("edit-section").style.display = "none";
    document.getElementById("detail-section").style.display = "block";
}

// 15. 게시글 수정 완료
async function updatePost() {
    const title = document.getElementById("edit-title").value;
    const content = document.getElementById("edit-content").value;
    const imageInput = document.getElementById("edit-imageFile");
    const token = localStorage.getItem("accessToken");

    if (!token) return alert("로그인이 필요합니다!");

    const formData = new FormData();
    formData.append("request", new Blob([JSON.stringify({ title, content })], { type: "application/json" }));
    if (imageInput.files.length > 0) formData.append("images", imageInput.files[0]);

    try {
        const response = await fetch(`${API_BASE}/posts/${currentPostId}`, {
            method: "PUT",
            headers: { "Authorization": "Bearer " + token },
            body: formData
        });

        if (response.ok) {
            alert("게시글이 성공적으로 수정되었습니다!");
            document.getElementById("edit-section").style.display = "none";
            viewPost(currentPostId);
        } else {
            alert("권한이 없습니다! (본인이 작성한 글만 수정 가능합니다)");
        }
    } catch (error) { console.error(error); }
}

// 창 켜질 때 실행
window.onload = function() {
    fetchPosts();
    if (localStorage.getItem("accessToken")) {
        document.getElementById("login-section").style.display = "none";
        document.getElementById("logout-section").style.display = "block";
    }
};