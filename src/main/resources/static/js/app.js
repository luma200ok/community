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
            <button class="btn btn-outline" style="padding: 5px 10px; font-size: 12px;" onclick="showMyPage()">👤 내 정보</button>
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
    ['login-section', 'signup-section', 'write-section', 'find-password-section','mypage-section'].forEach(id => {
        document.getElementById(id).style.display = 'none';
    });
}

function toggleForm(type) {
    hideAllForms(); // 열려있는 모든 폼을 싹 다 닫음

    // 화면이 전환되는 느낌을 위해 밑에 깔려있던 리스트와 상세페이지도 숨김
    document.getElementById('list-section').style.display = 'none';
    document.getElementById('detail-section').style.display = 'none';

    // 요청한(type) 화면만 딱 켜주기! (코드 통일성 확보)
    if (type === 'signup') document.getElementById('signup-section').style.display = 'block';
    if (type === 'login') document.getElementById('login-section').style.display = 'block';
    if (type === 'find-password') document.getElementById('find-password-section').style.display = 'block';

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
    const hintAnswer = document.getElementById("reg-hint").value;
    try {
        const response = await fetch(`${API_BASE}/users/signup`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password, email, hintAnswer })
        });
        if (response.ok) {
            alert("🎉 회원가입 성공! 이제 로그인해주세요.");
            toggleForm('login');

            // 폼 비우기 (청소!)
            document.getElementById("reg-username").value = "";
            document.getElementById("reg-password").value = "";
            document.getElementById("reg-email").value = "";
            document.getElementById("reg-hint").value = "";

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
            showList();
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
                            <span>👀 ${post.viewCount} | ❤️ ${post.likeCount} | 💬 ${post.commentCount}</span>                        </div>
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

        // 💡 1. 댓글과 대댓글을 그려주는 마법의 재귀 함수
        function generateCommentHtml(comment, isReply = false) {
            const isDeleted = comment.writer === "(알 수 없음)";

            // ⭐ [디테일 추가] 내용 맨 앞에 '@유저이름'이 있으면 파란색으로 예쁘게 강조해 주는 정규식!
            const formattedContent = comment.content.replace(/^(@\S+)/, '<span style="color: #007bff; font-weight: bold;">$1</span>');

            let html = `
                <div class="comment-box" style="${isReply ? 'margin-left: 40px; border-left: 3px solid #eee; padding-left: 15px;' : ''}">
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            ${isReply ? '↳ ' : ''}<b>${comment.writer}</b> 
                            <span style="font-size:0.8em; color:#999;">(${comment.createdAt})</span>
                        </div>
                        <div>
                            ${!isDeleted ? `<button class="btn btn-text" style="font-size: 12px; padding: 2px 5px;" onclick="showReplyInput(${comment.id}, ${isReply ? `'${comment.writer}'` : 'null'})">답글</button>` : ''}
                            
                            ${!isDeleted ? `<button class="btn btn-outline" style="padding: 2px 8px; font-size: 12px; color: #dc3545; border-color: #dc3545;" onclick="deleteComment(${comment.id})">삭제</button>` : ''}
                        </div>
                    </div>
                    
                    <div style="margin-top:5px; ${isDeleted ? 'color:#999; font-style:italic;' : ''}">${isDeleted ? comment.content : formattedContent}</div>
                    
                    <div id="reply-input-${comment.id}" style="display: none; margin-top: 10px; gap: 10px;">
                        <input type="text" id="reply-content-${comment.id}" class="input-box" placeholder="답글을 입력하세요" style="flex: 1; margin: 0; padding: 8px;">
                        <button class="btn btn-primary" style="padding: 8px 15px;" onclick="writeReply(${comment.id})">등록</button>
                    </div>
                </div>
            `;

            // 내 밑에 대댓글이 있다면 반복해서 그림 (들여쓰기는 1번만 적용됨)
            if (comment.replies && comment.replies.length > 0) {
                comment.replies.forEach(reply => {
                    html += generateCommentHtml(reply, true);
                });
            }
            return html;
        }

        // 💡 3. 최상위 댓글부터 순회하며 화면에 찍기
        post.comments.forEach(comment => {
            commentDiv.innerHTML += generateCommentHtml(comment, false);
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
        // 💡 게시글 번호(currentPostId)를 주소에 추가했습니다!
        const response = await fetch(`${API_BASE}/posts/${currentPostId}/comments/${commentId}`, { method: "DELETE", headers: headers });
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

// ==========================================
// 💡 6. 관리자 이스터에그 (로고 5번 클릭)
// ==========================================
let logoClickCount = 0;
let logoClickTimer = null;

function handleLogoClick() {
    showList(); // 💡 기본 기능인 '목록 새로고침'은 그대로 유지

    logoClickCount++;

    // 2초 안에 연속으로 안 누르면 카운트 초기화
    if (logoClickCount === 1) {
        logoClickTimer = setTimeout(() => { logoClickCount = 0; }, 2000);
    }

    // 5번 연속 클릭 성공!
    if (logoClickCount >= 5) {
        clearTimeout(logoClickTimer);
        logoClickCount = 0;
        triggerEasterEgg(); // 이스터에그 발동!
    }
}

async function triggerEasterEgg() {
    // 1. 누구를 승급시킬지 묻기
    const targetUsername = prompt("🔥 [Admin Mode] 관리자로 승급시킬 유저의 아이디를 입력하세요:");
    if (!targetUsername) return;

    // 2. 시크릿 키 묻기
    const secretKey = prompt("🔥 [Admin Mode] 시크릿 키를 입력하세요:");
    if (!secretKey) return;

    // 3. 백엔드 비밀 API 호출!
    try {
        const headers = getAuthHeaders();
        const response = await fetch(`${API_BASE}/users/promote?username=${encodeURIComponent(targetUsername)}&secretKey=${encodeURIComponent(secretKey)}`, {
            method: "POST",
            headers: headers
        });

        if (response.ok) {
            const msg = await response.text();
            alert("🎉 " + msg);
            location.reload(); // 성공 시 새로고침하여 관리자 권한 즉시 적용
        } else {
            alert("❌ 권한이 없거나 잘못된 입력입니다.");
        }
    } catch (error) {
        console.error(error);
    }
}

// ==========================================
// 💡 7. 대댓글(답글) UI 컨트롤 및 등록 로직
// ==========================================

// 답글 창 보이기/숨기기 토글
function showReplyInput(commentId, targetWriter) {
    const inputDiv = document.getElementById(`reply-input-${commentId}`);
    if (inputDiv.style.display === "none") {
        inputDiv.style.display = "flex";
        const inputField = document.getElementById(`reply-content-${commentId}`);

        // ⭐ 대댓글에 답글을 다는 경우, 입력창에 '@작성자 '를 미리 쳐줍니다!
        if (targetWriter) {
            inputField.value = `@${targetWriter} `;
        } else {
            inputField.value = ""; // 일반 부모 댓글이면 빈칸
        }
        inputField.focus(); // 마우스 클릭 없이 바로 타자 칠 수 있게 커서 자동 깜빡임!
    } else {
        inputDiv.style.display = "none";
    }
}

// 대댓글(답글) 작성 API 호출
async function writeReply(parentId) {
    const content = document.getElementById(`reply-content-${parentId}`).value;
    const headers = getAuthHeaders();

    if (!headers.Authorization) return alert("로그인이 필요합니다!");
    if (!content.trim()) return alert("답글 내용을 입력하세요.");

    headers["Content-Type"] = "application/json";

    try {
        const response = await fetch(`${API_BASE}/posts/${currentPostId}/comments/${parentId}/replies`, {
            method: "POST",
            headers: headers,
            body: JSON.stringify({ content })
        });

        if (response.ok) {
            // 성공하면 게시글 상세 화면을 새로고침하여 바뀐 댓글 목록을 보여줌
            viewPost(currentPostId);
        } else {
            alert("답글 작성 실패!");
        }
    } catch (error) { console.error(error); }
}

// ==========================================
// 💡 8. 비밀번호 찾기 (임시 비밀번호 이메일 발송)
// ==========================================
async function findPassword() {
    const username = document.getElementById("find-username").value;
    const email = document.getElementById("find-email").value;
    const hintAnswer = document.getElementById("find-hint").value;

    if (!username || !email || !hintAnswer) {
        return alert("모든 항목을 입력해주세요.");
    }

    alert("이메일 발송을 요청했습니다. 잠시만 기다려주세요... 🚀");

    try {
        const response = await fetch(`${API_BASE}/users/password/find`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, email, hintAnswer })
        });

        if (response.ok) {
            const msg = await response.text();
            alert("🎉 " + msg);

            //  폼 비우기 (청소!)
            document.getElementById("find-username").value = "";
            document.getElementById("find-email").value = "";
            document.getElementById("find-hint").value = "";

            toggleForm('login'); // 성공하면 로그인 창으로 이동
        } else {
            // 에러 메시지(힌트 틀림, 5분 쿨타임 등) 띄워주기
            const errorData = await response.json();
            alert("❌ " + (errorData.message || "정보가 일치하지 않습니다."));
        }
    } catch (error) {
        console.error(error);
        alert("❌ 서버 통신 중 오류가 발생했습니다.");
    }
}
// ==========================================
// 💡 9. 마이페이지 (기본 정보 + 활동 내역 통합 조회)
// ==========================================
async function showMyPage() {
    const headers = getAuthHeaders();
    if (!headers.Authorization) return alert("로그인이 필요합니다!");

    try {
        // 1. 기본 정보 조회 (/api/mypage/info)
        const infoRes = await fetch(`${API_BASE}/mypage/info`, { headers });
        if (infoRes.ok) {
            const userData = await infoRes.json();
            document.getElementById("my-username").innerText = userData.username;
            document.getElementById("my-email").innerText = userData.email;
            document.getElementById("my-hint").innerText = userData.hintAnswer;
            document.getElementById("my-role").innerHTML = userData.role === "ADMIN"
                ? `<span style="color: red; font-weight: bold;">👑 관리자</span>`
                : "👤 일반 회원";
        }

        // 2. 활동 내역 조회 (Promise.all로 병렬 호출하여 속도 업!)
        // 백엔드 MyPageController의 @GetMapping("/posts", "/comments", "/likes")를 각각 찌릅니다.
        const [postsRes, commentsRes, likesRes] = await Promise.all([
            fetch(`${API_BASE}/mypage/posts?page=0&size=5`, { headers }),
            fetch(`${API_BASE}/mypage/comments?page=0&size=5`, { headers }),
            fetch(`${API_BASE}/mypage/likes?page=0&size=5`, { headers })
        ]);

        // 3. 내가 쓴 게시글 렌더링 (Spring Page 객체이므로 .content에 데이터가 있음)
        if (postsRes.ok) {
            const data = await postsRes.json();
            const postsDiv = document.getElementById("my-posts-list");
            postsDiv.innerHTML = data.content.length > 0
                ? data.content.map(p => `<div class="link-text" onclick="viewPost(${p.id})" style="margin-bottom:5px;">• ${p.title}</div>`).join('')
                : "작성한 게시글이 없습니다.";
        }

        // 4. 내가 쓴 댓글 렌더링
        if (commentsRes.ok) {
            const data = await commentsRes.json();
            const commentsDiv = document.getElementById("my-comments-list");
            commentsDiv.innerHTML = data.content.length > 0
                ? data.content.map(c => `
        <div class="link-text" onclick="viewPost(${c.postId})" style="margin-bottom:8px; padding:10px; background:#f8f9fa; border-radius:6px; cursor:pointer;">
            <strong>${c.content}</strong>
        </div>
    `).join('')
                : "작성한 댓글이 없습니다.";
        }

        // 5. 좋아요 누른 게시글 렌더링
        if (likesRes.ok) {
            const data = await likesRes.json();
            const likesDiv = document.getElementById("my-liked-list");
            likesDiv.innerHTML = data.content.length > 0
                ? data.content.map(p => `<div class="link-text" onclick="viewPost(${p.id})" style="margin-bottom:5px;">❤️ ${p.title}</div>`).join('')
                : "좋아요 표시한 게시글이 없습니다.";
        }

        // 화면 전환
        hideAllForms();
        document.getElementById('list-section').style.display = 'none';
        document.getElementById('detail-section').style.display = 'none';
        document.getElementById("mypage-section").style.display = "block";

    } catch (error) {
        console.error("마이페이지 로딩 에러:", error);
        alert("데이터를 불러오는 중 오류가 발생했습니다.");
    }
}

// ==========================================
// 💡 10. 내 정보 수정 (비밀번호, 힌트)
// ==========================================

// 수정 폼 토글
function toggleUpdateForm() {
    const form = document.getElementById("update-form");
    form.style.display = form.style.display === "none" ? "block" : "none";
}

// 실제 수정 API 호출
async function updateUserInfo() {
    // 💡 const를 let으로 변경하여 변수 값을 중간에 바꿀 수 있게 합니다!
    let currentPassword = document.getElementById("upd-current-pw").value;
    const newPassword = document.getElementById("upd-new-pw").value;
    const newHintAnswer = document.getElementById("upd-new-hint").value;

    if (!currentPassword) {
        return alert("보안을 위해 현재 비밀번호를 반드시 입력해야 합니다.");
    }

    const headers = getAuthHeaders();
    headers["Content-Type"] = "application/json";

    try {
        // 1. 비밀번호 변경
        if (newPassword && newPassword.trim() !== "") {
            const pwRes = await fetch(`${API_BASE}/mypage/password`, {
                method: "PATCH",
                headers: headers,
                body: JSON.stringify({ currentPassword, newPassword })
            });
            if (!pwRes.ok) throw new Error("비밀번호 수정 실패");

            // ⭐ 핵심 로직 추가: 비밀번호가 성공적으로 바뀌었다면,
            // 2번(힌트) API 본인 인증을 무사히 통과하기 위해 현재 비밀번호를 방금 바꾼 새 비밀번호로 덮어씌웁니다!
            currentPassword = newPassword;
        }

        // 2. 힌트 정답 변경
        if (newHintAnswer && newHintAnswer.trim() !== "") {
            const hintRes = await fetch(`${API_BASE}/mypage/hint`, {
                method: "PATCH",
                headers: headers,
                body: JSON.stringify({ currentPassword, newHintAnswer }) // 💡 이제 업데이트된 비번이 날아감!
            });
            if (!hintRes.ok) throw new Error("힌트 수정 실패");
        }

        alert("🎉 정보가 성공적으로 수정되었습니다! 보안을 위해 다시 로그인 해주세요.");

        // 입력 필드 초기화 및 로그아웃
        document.getElementById("upd-current-pw").value = "";
        document.getElementById("upd-new-pw").value = "";
        document.getElementById("upd-new-hint").value = "";

        logout();

    } catch (error) {
        console.error(error);
        alert("❌ 수정 실패: 현재 비밀번호가 틀렸거나 통신 오류가 발생했습니다.");
    }
}