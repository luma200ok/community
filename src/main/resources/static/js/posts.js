// =========================================
// 게시글 / 댓글 모듈
// =========================================
import { API_BASE, fetchWithAuth, getAuthHeaders } from './api.js';
import { showToast } from './toast.js';
import { navigate } from './router.js';
import { showSection, showList, escapeHtml } from './ui.js';

let currentPostId = null;
let currentPostCategory = '자유';
let currentPostRawContent = '';   // 수정 폼 복원용 원본 마크다운
export let accumulatedFiles = [];

export function getCurrentPostId() { return currentPostId; }

export function prepareWriteForm() {
    document.getElementById('category').value = '자유';
    document.getElementById('title').value = '';
    document.getElementById('content').value = '';
    document.getElementById('imageFile').value = '';
    accumulatedFiles = [];
    document.getElementById('write-file-count').innerHTML = '';
    document.getElementById('write-image-preview').innerHTML = '';
    showSection('write');
}

// ==========================================
// 게시글 목록
// ==========================================
export async function fetchPosts(page = 0) {
    try {
        const keyword = document.getElementById('search-keyword').value;
        const url = keyword.trim()
            ? `${API_BASE}/posts?keyword=${encodeURIComponent(keyword)}&page=${page}&size=9`
            : `${API_BASE}/posts?page=${page}&size=9`;

        const response = await fetch(url);
        const data = await response.json();
        const posts = data.content;

        const postListDiv = document.getElementById('post-list');
        postListDiv.innerHTML = '';

        if (posts.length === 0) {
            postListDiv.innerHTML = "<p style='grid-column: 1/-1; text-align:center; color:#777;'>게시글이 없습니다.</p>";
            document.getElementById('pagination').innerHTML = '';
            return;
        }

        posts.forEach(post => {
            let badgeStyle = 'background-color: #007bff; color: white;';
            if (post.category === '공지')   badgeStyle = 'background-color: #dc3545; color: white; font-weight: bold;';
            else if (post.category === '질문') badgeStyle = 'background-color: #28a745; color: white;';

            const thumbHtml = post.thumbnailUrl
                ? `<img src="${escapeHtml(post.thumbnailUrl)}" class="card-image" alt="썸네일">`
                : `<div class="card-image-placeholder"></div>`;

            postListDiv.innerHTML += `
                <div class="card" data-id="${post.id}">
                    ${thumbHtml}
                    <div class="card-content">
                        <span class="card-badge" style="${badgeStyle}">${escapeHtml(post.category)}</span>
                        <h3 class="card-title">${escapeHtml(post.title)}</h3>
                        <p class="card-summary">내용을 보려면 클릭하세요...</p>
                        <div class="card-footer">
                            <span>👤 ${escapeHtml(post.writer)}</span>
                            <span>👀 ${post.viewCount} | ❤️ ${post.likeCount} | 💬 ${post.commentCount}</span>
                        </div>
                    </div>
                </div>`;
        });

        postListDiv.querySelectorAll('.card').forEach(card => {
            card.addEventListener('click', () => navigate('posts/' + card.dataset.id));
        });

        renderPagination(data.totalPages, data.number);
    } catch (error) {
        console.error(error);
    }
}

// ==========================================
// 게시글 상세
// ==========================================
export async function viewPost(postId) {
    currentPostId = postId;
    const headers = getAuthHeaders();

    try {
        const response = await fetch(`${API_BASE}/posts/${postId}`, { headers });
        if (!response.ok) {
            showToast('게시글을 불러올 수 없습니다.', 'error');
            return;
        }
        const post = await response.json();
        currentPostCategory = post.category;
        currentPostRawContent = post.content;

        showSection('detail');

        document.getElementById('detail-title').innerText = post.title;
        document.getElementById('detail-meta').innerText =
            `👤 작성자: ${post.writer} | 👀 조회: ${post.viewCount} | 📅 ${post.createdAt}`;

        // 마크다운 렌더링 (DOMPurify로 XSS 방지)
        const contentEl = document.getElementById('detail-content');
        if (window.marked && window.DOMPurify) {
            contentEl.style.whiteSpace = 'normal';
            contentEl.innerHTML = DOMPurify.sanitize(marked.parse(post.content));
        } else {
            contentEl.innerText = post.content;
        }

        // 첨부 파일
        const imgDiv = document.getElementById('detail-images');
        imgDiv.innerHTML = '';
        post.imageUrls.forEach(url => {
            if (url.toLowerCase().endsWith('.pdf')) {
                imgDiv.innerHTML += `
                    <div style="margin-bottom:15px;padding:15px;border:1px solid #ddd;border-radius:8px;background:#fff5f5;display:inline-block;">
                        <span style="font-size:20px;vertical-align:middle;">📄</span>
                        <a href="${url}" target="_blank" rel="noopener noreferrer" style="color:#dc3545;text-decoration:none;font-weight:bold;margin-left:8px;">
                            첨부된 PDF 파일 보기 / 다운로드
                        </a>
                    </div><br>`;
            } else {
                imgDiv.innerHTML += `<img src="${url}" style="max-width:100%;border-radius:8px;margin-bottom:15px;" alt="첨부 이미지">`;
            }
        });

        // 좋아요 버튼
        const likeBtn = document.getElementById('like-btn');
        likeBtn.innerText = post.isLiked
            ? `❤️ 좋아요 취소 (${post.likeCount})`
            : `🤍 좋아요 (${post.likeCount})`;
        likeBtn.style.backgroundColor = post.isLiked ? '#ff4d4d' : '#6c757d';

        // 댓글
        renderComments(post.comments);

    } catch (error) {
        console.error(error);
    }
}

// ==========================================
// 댓글 렌더링
// ==========================================
function renderComments(comments) {
    const commentDiv = document.getElementById('comment-list');
    commentDiv.innerHTML = '';

    if (comments.length === 0) {
        commentDiv.innerHTML = "<p style='color:#777;'>첫 번째 댓글을 남겨보세요!</p>";
    }

    function buildCommentHtml(comment, isReply = false) {
        const isDeleted = comment.writer === '(알 수 없음)';
        const hasActiveReplies = comment.replies?.some(r => r.writer !== '(알 수 없음)');
        if (isDeleted && !hasActiveReplies) return '';

        const escapedContent = escapeHtml(comment.content);
        const formattedContent = escapedContent.replace(/^(@\S+)/, '<span style="color:#007bff;font-weight:bold;">$1</span>');

        let html = `
            <div class="comment-box" style="${isReply ? 'margin-left:40px;border-left:3px solid #eee;padding-left:15px;' : ''}">
                <div style="display:flex;justify-content:space-between;align-items:center;">
                    <div>
                        ${isReply ? '↳ ' : ''}<b>${escapeHtml(comment.writer)}</b>
                        <span style="font-size:0.8em;color:#999;">(${escapeHtml(comment.createdAt)})</span>
                    </div>
                    <div>
                        ${!isDeleted ? `<button class="btn btn-text" style="font-size:12px;padding:2px 5px;" data-reply-toggle="${comment.id}" data-writer="${escapeHtml(isReply ? comment.writer : '')}">답글</button>` : ''}
                        ${!isDeleted ? `<button class="btn btn-outline" style="padding:2px 8px;font-size:12px;color:#dc3545;border-color:#dc3545;" data-delete-comment="${comment.id}">삭제</button>` : ''}
                    </div>
                </div>
                <div style="margin-top:5px;${isDeleted ? 'color:#999;font-style:italic;' : ''}">${isDeleted ? escapedContent : formattedContent}</div>
                <div id="reply-input-${comment.id}" style="display:none;margin-top:10px;gap:10px;">
                    <input type="text" id="reply-content-${comment.id}" class="input-box" placeholder="답글을 입력하세요" style="flex:1;margin:0;padding:8px;">
                    <button class="btn btn-primary" style="padding:8px 15px;" data-write-reply="${comment.id}">등록</button>
                </div>
            </div>`;

        comment.replies?.forEach(reply => { html += buildCommentHtml(reply, true); });
        return html;
    }

    comments.forEach(c => { commentDiv.innerHTML += buildCommentHtml(c); });

    // 이벤트 위임 (렌더링 후 일괄 바인딩)
    commentDiv.querySelectorAll('[data-reply-toggle]').forEach(btn => {
        btn.addEventListener('click', () => toggleReplyInput(btn.dataset.replyToggle, btn.dataset.writer));
    });
    commentDiv.querySelectorAll('[data-delete-comment]').forEach(btn => {
        btn.addEventListener('click', () => deleteComment(btn.dataset.deleteComment));
    });
    commentDiv.querySelectorAll('[data-write-reply]').forEach(btn => {
        btn.addEventListener('click', () => writeReply(btn.dataset.writeReply));
    });
}

function toggleReplyInput(commentId, targetWriter) {
    const inputDiv = document.getElementById(`reply-input-${commentId}`);
    if (inputDiv.style.display === 'none') {
        inputDiv.style.display = 'flex';
        const inputField = document.getElementById(`reply-content-${commentId}`);
        inputField.value = targetWriter ? `@${targetWriter} ` : '';
        inputField.focus();
    } else {
        inputDiv.style.display = 'none';
    }
}

export async function writeComment() {
    const content = document.getElementById('comment-input').value;
    const headers = getAuthHeaders();
    if (!headers.Authorization) { showToast('로그인이 필요합니다.', 'error'); return; }
    if (!content.trim()) { showToast('댓글 내용을 입력하세요.', 'error'); return; }

    headers['Content-Type'] = 'application/json';
    try {
        const response = await fetchWithAuth(`${API_BASE}/posts/${currentPostId}/comments`, {
            method: 'POST', headers, body: JSON.stringify({ content })
        });
        if (response.ok) {
            document.getElementById('comment-input').value = '';
            viewPost(currentPostId);
        } else {
            showToast('댓글 작성 실패!', 'error');
        }
    } catch (error) { console.error(error); }
}

async function writeReply(parentId) {
    const content = document.getElementById(`reply-content-${parentId}`).value;
    const headers = getAuthHeaders();
    if (!headers.Authorization) { showToast('로그인이 필요합니다.', 'error'); return; }
    if (!content.trim()) { showToast('답글 내용을 입력하세요.', 'error'); return; }

    headers['Content-Type'] = 'application/json';
    try {
        const response = await fetchWithAuth(
            `${API_BASE}/posts/${currentPostId}/comments/${parentId}/replies`,
            { method: 'POST', headers, body: JSON.stringify({ content }) }
        );
        if (response.ok) viewPost(currentPostId);
        else showToast('답글 작성 실패!', 'error');
    } catch (error) { console.error(error); }
}

async function deleteComment(commentId) {
    if (!confirm('이 댓글을 삭제하시겠습니까?')) return;
    const headers = getAuthHeaders();
    if (!headers.Authorization) { showToast('로그인이 필요합니다.', 'error'); return; }
    try {
        const response = await fetchWithAuth(
            `${API_BASE}/posts/${currentPostId}/comments/${commentId}`,
            { method: 'DELETE', headers }
        );
        if (response.ok) viewPost(currentPostId);
        else showToast('권한이 없습니다!', 'error');
    } catch (error) { console.error(error); }
}

export async function toggleLike() {
    const headers = getAuthHeaders();
    if (!headers.Authorization) { showToast('로그인이 필요합니다.', 'error'); return; }
    try {
        await fetchWithAuth(`${API_BASE}/posts/${currentPostId}/likes`, { method: 'POST', headers });
        viewPost(currentPostId);
    } catch (error) { console.error(error); }
}

// ==========================================
// 게시글 작성
// ==========================================
export async function writePost() {
    const category = document.getElementById('category').value;
    const title    = document.getElementById('title').value;
    const content  = document.getElementById('content').value;
    const headers  = getAuthHeaders();
    if (!headers.Authorization) { showToast('로그인이 필요합니다.', 'error'); return; }

    const formData = new FormData();
    formData.append('request', new Blob([JSON.stringify({ title, content, category })], { type: 'application/json' }));
    accumulatedFiles.forEach(file => formData.append('images', file));

    try {
        const response = await fetchWithAuth(`${API_BASE}/posts`, { method: 'POST', body: formData });
        if (response.ok) {
            showToast('게시글 등록 완료!', 'success');
            accumulatedFiles = [];
            navigate('');
        } else {
            const err = await response.json();
            showToast('작성 실패: ' + (err.message || '권한이 없습니다.'), 'error');
        }
    } catch (error) { console.error(error); }
}

// ==========================================
// 게시글 수정
// ==========================================
export function showEditForm() {
    document.getElementById('edit-title').value    = document.getElementById('detail-title').innerText;
    document.getElementById('edit-content').value  = currentPostRawContent;
    document.getElementById('edit-category').value = currentPostCategory;
    document.getElementById('edit-imageFile').value = '';
    accumulatedFiles = [];
    document.getElementById('edit-file-count').innerHTML = '';
    document.getElementById('edit-image-preview').innerHTML = '';
    showSection('edit');
}

export function cancelEdit() {
    showSection('detail');
}

export async function updatePost() {
    const category = document.getElementById('edit-category').value;
    const title    = document.getElementById('edit-title').value;
    const content  = document.getElementById('edit-content').value;
    const headers  = getAuthHeaders();
    if (!headers.Authorization) { showToast('로그인이 필요합니다.', 'error'); return; }

    const formData = new FormData();
    formData.append('request', new Blob([JSON.stringify({ title, content, category })], { type: 'application/json' }));
    accumulatedFiles.forEach(file => formData.append('images', file));

    try {
        const response = await fetchWithAuth(`${API_BASE}/posts/${currentPostId}`, { method: 'PUT', body: formData });
        if (response.ok) {
            showToast('수정 성공!', 'success');
            await viewPost(currentPostId);
        } else {
            const err = await response.json().catch(() => ({}));
            showToast('수정 실패: ' + (err.message || '권한이 없습니다.'), 'error');
        }
    } catch (error) { console.error(error); }
}

// ==========================================
// 게시글 삭제
// ==========================================
export async function deletePost() {
    if (!confirm('정말 이 게시글을 삭제하시겠습니까?')) return;
    const headers = getAuthHeaders();
    if (!headers.Authorization) { showToast('로그인이 필요합니다.', 'error'); return; }
    try {
        const response = await fetchWithAuth(`${API_BASE}/posts/${currentPostId}`, { method: 'DELETE', headers });
        if (response.ok) {
            showToast('게시글이 삭제되었습니다.', 'success');
            navigate('');
        } else {
            showToast('권한이 없습니다!', 'error');
        }
    } catch (error) { console.error(error); }
}

// ==========================================
// 파일 업로드 미리보기
// ==========================================
export function handleFileSelect(input, previewBoxId, countBoxId) {
    if (!input.files || input.files.length === 0) return;
    Array.from(input.files).forEach(file => accumulatedFiles.push(file));
    input.value = '';
    updatePreviewUI(previewBoxId, countBoxId);
}

export function updatePreviewUI(previewBoxId, countBoxId) {
    const previewBox = document.getElementById(previewBoxId);
    const countBox   = document.getElementById(countBoxId);
    previewBox.innerHTML = '';

    if (accumulatedFiles.length === 0) {
        countBox.innerHTML = '';
        return;
    }

    countBox.innerHTML = `✅ 총 <b>${accumulatedFiles.length}개</b>의 파일이 선택되었습니다.`;

    accumulatedFiles.forEach((file, index) => {
        const div = document.createElement('div');
        div.style.cssText = 'position:relative;display:inline-block;';

        if (file.type.startsWith('image/')) {
            const reader = new FileReader();
            reader.onload = (e) => {
                const img = document.createElement('img');
                img.src = e.target.result;
                img.style.cssText = 'width:100px;height:100px;object-fit:cover;border-radius:8px;box-shadow:0 2px 5px rgba(0,0,0,0.2);';
                div.appendChild(img);
            };
            reader.readAsDataURL(file);
        } else if (file.type === 'application/pdf') {
            const pdfBox = document.createElement('div');
            const shortName = file.name.length > 10 ? file.name.substring(0, 10) + '...' : file.name;
            pdfBox.style.cssText = 'width:100px;height:100px;background:#ffcccc;color:#cc0000;display:flex;flex-direction:column;align-items:center;justify-content:center;border-radius:8px;box-shadow:0 2px 5px rgba(0,0,0,0.2);padding:5px;box-sizing:border-box;text-align:center;';
            pdfBox.innerHTML = `<span style="font-size:24px;">📄</span><br><span style="font-size:11px;font-weight:bold;margin-top:5px;word-break:break-all;">${shortName}</span>`;
            div.appendChild(pdfBox);
        }

        const capturedFile = file;
        const delBtn = document.createElement('button');
        delBtn.innerHTML = '✕';
        delBtn.style.cssText = 'position:absolute;top:5px;right:5px;background:rgba(255,0,0,0.8);color:white;border:none;border-radius:50%;width:22px;height:22px;font-size:12px;cursor:pointer;z-index:10;line-height:1;';
        delBtn.addEventListener('click', () => {
            accumulatedFiles = accumulatedFiles.filter(f => f !== capturedFile);
            updatePreviewUI(previewBoxId, countBoxId);
        });
        div.appendChild(delBtn);
        previewBox.appendChild(div);
    });
}

// ==========================================
// 페이지네이션
// ==========================================
function renderPagination(totalPages, currentPage) {
    const paginationDiv = document.getElementById('pagination');
    paginationDiv.innerHTML = '';
    if (totalPages <= 1) return;

    for (let i = 0; i < totalPages; i++) {
        const btn = document.createElement('button');
        btn.innerText = i + 1;
        if (i === currentPage) {
            btn.className = 'btn btn-primary';
            btn.style.cursor = 'default';
        } else {
            btn.className = 'btn btn-outline';
            btn.addEventListener('click', () => {
                fetchPosts(i);
                window.scrollTo(0, 0);
            });
        }
        paginationDiv.appendChild(btn);
    }
}
