
// Hàm lấy danh sách đánh giá từ API
async function fetchReviews(page = 1, search = '') {
    try {
        const response = await fetch(`/api/quanly/reviews?page=${page}&search=${encodeURIComponent(search)}`, {
            headers: {
                'Content-Type': 'application/json',
                [document.querySelector('meta[name="_csrf_header"]').getAttribute('content')]:
                    document.querySelector('meta[name="_csrf"]').getAttribute('content')
            }
        });
        if (!response.ok) throw new Error('Không thể lấy danh sách đánh giá.');
        const data = await response.json();
        reviews = data.reviews;
        updateReviewsTable();
        updatePagination(data.totalPages, page);
    } catch (error) {
        Swal.fire('Lỗi', error.message, 'error');
    }
}

// Hàm cập nhật bảng đánh giá
function updateReviewsTable() {
    const tbody = document.getElementById('reviews-tbody');
    tbody.innerHTML = reviews.map(review => `
        <tr>
            <td>${review.id}</td>
            <td>${new Date(review.created_at).toLocaleString('vi-VN')}</td>
            <td>${review.rating}</td>
            <td>${review.review_content}</td>
            <td>${new Date(review.updated_at).toLocaleString('vi-VN')}</td>
            <td>${review.product_name || 'Không xác định'}</td>
            <td>${review.username || 'Không xác định'}</td>
            <td>
                <button class="btn btn-success btn-sm approve-btn" 
                        onclick="approveReview(${review.id})" 
                        data-approved="${review.approved}" 
                        ${review.approved ? 'disabled' : ''}>
                    ${review.approved ? 'Đã duyệt' : 'Duyệt'}
                </button>
                <button class="btn btn-primary btn-sm reply-btn" 
                        onclick="showReplyInterface(${review.id})">
                    Trả lời
                </button>
            </td>
        </tr>
    `).join('');
}


// Hàm duyệt đánh giá
function approveReview(reviewId) {
    const review = reviews.find(r => r.id === reviewId);
    if (!review) {
        Swal.fire('Lỗi', 'Không tìm thấy đánh giá.', 'error');
        return;
    }

    if (review.approved) {
        Swal.fire('Thông báo', 'Đánh giá này đã được duyệt.', 'info');
        return;
    }

    Swal.fire({
        title: 'Xác nhận duyệt đánh giá?',
        text: 'Đánh giá sẽ được hiển thị công khai sau khi duyệt.',
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#3085d6',
        cancelButtonColor: '#d33',
        confirmButtonText: 'Duyệt',
        cancelButtonText: 'Hủy'
    }).then(async (result) => {
        if (result.isConfirmed) {
            try {
                const response = await fetch(`/api/quanly/reviews/${reviewId}/approve`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        [document.querySelector('meta[name="_csrf_header"]').getAttribute('content')]:
                            document.querySelector('meta[name="_csrf"]').getAttribute('content')
                    }
                });
                if (!response.ok) throw new Error('Không thể duyệt đánh giá.');
                review.approved = true;
                updateReviewsTable();
                Toastify({
                    text: `Đánh giá ID ${reviewId} đã được duyệt.`,
                    duration: 2000,
                    gravity: 'top',
                    position: 'right',
                    backgroundColor: '#28a745'
                }).showToast();
            } catch (error) {
                Swal.fire('Lỗi', error.message, 'error');
            }
        }
    });
}

// Hàm hiển thị giao diện trả lời
function showReplyInterface(reviewId) {
    const review = reviews.find(r => r.id === reviewId);
    if (!review) {
        Swal.fire('Lỗi', 'Không tìm thấy đánh giá.', 'error');
        return;
    }

    const reviewsTable = document.getElementById('reviews-table');
    const replyInterface = document.getElementById('reply-interface');

    reviewsTable.classList.add('fade-out');
    setTimeout(() => {
        reviewsTable.style.display = 'none';
        reviewsTable.classList.remove('fade-out');

        replyInterface.style.display = 'block';
        replyInterface.classList.add('fade-in');
        setTimeout(() => replyInterface.classList.remove('fade-in'), 300);
    }, 300);

    // Cập nhật bảng thu gọn
    const tbodyCompact = document.getElementById('reviews-tbody-compact');
    tbodyCompact.innerHTML = reviews.map(r => `
        <tr>
            <td>${r.id}</td>
            <td>${new Date(r.created_at).toLocaleString('vi-VN')}</td>
            <td>${r.rating}</td>
            <td>${r.review_content}</td>
            <td>${r.username || 'Không xác định'}</td>
        </tr>
    `).join('');

    // Cập nhật khu vực hội thoại
    document.getElementById('current-review-username').textContent = review.username || 'Không xác định';
    const chatMessages = document.getElementById('chat-messages');
    chatMessages.innerHTML = `
        <div class="chat-message user">
            <p class="message-content">${review.review_content}</p>
            <p class="message-user">${review.username || 'Không xác định'}</p>
            <small class="message-time">${new Date(review.created_at).toLocaleString('vi-VN')}</small>
        </div>
    `;
    if (review.reply) {
        chatMessages.innerHTML += `
            <div class="chat-message admin">
                <p class="message-content">${review.reply}</p>
                <p class="message-user">Admin</p>
                <small class="message-time">${new Date().toLocaleString('vi-VN')}</small>
            </div>
        `;
    }
    chatMessages.scrollTop = chatMessages.scrollHeight;

    // Lưu reviewId vào thuộc tính data để sử dụng trong sendReply
    replyInterface.dataset.reviewId = reviewId;
}


// Hàm ẩn giao diện trả lời
function hideReplyInterface() {
    const reviewsTable = document.getElementById('reviews-table');
    const replyInterface = document.getElementById('reply-interface');

    replyInterface.classList.add('fade-out');
    setTimeout(() => {
        replyInterface.style.display = 'none';
        replyInterface.classList.remove('fade-out');
        replyInterface.dataset.reviewId = ''; // Xóa reviewId

        reviewsTable.style.display = 'table';
        reviewsTable.classList.add('fade-in');
        setTimeout(() => reviewsTable.classList.remove('fade-in'), 300);
    }, 300);

    document.getElementById('reply-content').value = '';
}

// Hàm gửi trả lời
async function sendReply() {
    const replyInterface = document.getElementById('reply-interface');
    const reviewId = parseInt(replyInterface.dataset.reviewId);
    const replyContent = document.getElementById('reply-content').value.trim();

    if (!replyContent) {
        Swal.fire('Lỗi', 'Vui lòng nhập nội dung trả lời.', 'error');
        return;
    }

    const review = reviews.find(r => r.id === reviewId);
    if (!review) {
        Swal.fire('Lỗi', 'Không tìm thấy đánh giá.', 'error');
        return;
    }

    Swal.fire({
        title: 'Xác nhận gửi trả lời?',
        text: 'Phản hồi sẽ được gửi và hiển thị công khai.',
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#3085d6',
        cancelButtonColor: '#d33',
        confirmButtonText: 'Gửi',
        cancelButtonText: 'Hủy'
    }).then(async (result) => {
        if (result.isConfirmed) {
            try {
                const response = await fetch(`/api/quanly/reviews/${reviewId}/reply`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        [document.querySelector('meta[name="_csrf_header"]').getAttribute('content')]:
                            document.querySelector('meta[name="_csrf"]').getAttribute('content')
                    },
                    body: JSON.stringify({ replyContent })
                });
                if (!response.ok) throw new Error('Không thể gửi trả lời.');
                review.reply = replyContent;

                const chatMessages = document.getElementById('chat-messages');
                chatMessages.innerHTML += `
                    <div class="chat-message admin">
                        <p class="message-content">${replyContent}</p>
                        <p class="message-user">Admin</p>
                        <small class="message-time">${new Date().toLocaleString('vi-VN')}</small>
                    </div>
                `;
                chatMessages.scrollTop = chatMessages.scrollHeight;

                Toastify({
                    text: `Đã gửi trả lời cho đánh giá của ${review.username || 'Người dùng không xác định'}.`,
                    duration: 2000,
                    gravity: 'top',
                    position: 'right',
                    backgroundColor: '#28a745'
                }).showToast();

                document.getElementById('reply-content').value = '';
            } catch (error) {
                Swal.fire('Lỗi', error.message, 'error');
            }
        }
    });
}

// Hàm cập nhật phân trang
function updatePagination(totalPages, currentPage) {
    const pagination = document.getElementById('reviews-pagination');
    pagination.innerHTML = '';
    for (let i = 1; i <= totalPages; i++) {
        pagination.innerHTML += `
            <li class="page-item ${i === currentPage ? 'active' : ''}">
                <a class="page-link" href="#" onclick="fetchReviews(${i}, document.getElementById('search-reviews').value)">${i}</a>
            </li>
        `;
    }
}

// Hàm tìm kiếm
document.getElementById('search-reviews').addEventListener('input', function() {
    fetchReviews(1, this.value);
});

// Hàm xuất PDF
document.getElementById('exportReviewsPDF').addEventListener('click', function(e) {
    e.preventDefault();
    const { jsPDF } = window.jspdf;
    const doc = new jsPDF();
    doc.autoTable({
        head: [['ID', 'Ngày tạo', 'Điểm', 'Nội dung', 'Ngày cập nhật', 'Tên sản phẩm', 'Tên người dùng']],
        body: reviews.map(r => [
            r.id,
            new Date(r.created_at).toLocaleString('vi-VN'),
            r.rating,
            r.review_content,
            new Date(r.updated_at).toLocaleString('vi-VN'),
            r.product_name || 'Không xác định',
            r.username || 'Không xác định'
        ])
    });
    doc.save('danh_sach_danh_gia.pdf');
});


// Hàm xuất Excel
document.getElementById('exportReviewsExcel').addEventListener('click', function(e) {
    e.preventDefault();
    const workbook = new ExcelJS.Workbook();
    const sheet = workbook.addWorksheet('Danh sách đánh giá');
    sheet.columns = [
        { header: 'ID', key: 'id' },
        { header: 'Ngày tạo', key: 'created_at' },
        { header: 'Điểm', key: 'rating' },
        { header: 'Nội dung', key: 'review_content' },
        { header: 'Ngày cập nhật', key: 'updated_at' },
        { header: 'Tên sản phẩm', key: 'product_name' },
        { header: 'Tên người dùng', key: 'username' }
    ];
    reviews.forEach(r => {
        sheet.addRow({
            id: r.id,
            created_at: new Date(r.created_at).toLocaleString('vi-VN'),
            rating: r.rating,
            review_content: r.review_content,
            updated_at: new Date(r.updated_at).toLocaleString('vi-VN'),
            product_name: r.product_name || 'Không xác định',
            username: r.username || 'Không xác định'
        });
    });
    workbook.xlsx.writeBuffer().then(buffer => {
        const blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'danh_sach_danh_gia.xlsx';
        a.click();
        window.URL.revokeObjectURL(url);
    });
});


// Khởi tạo khi tải trang
document.addEventListener('DOMContentLoaded', () => {
    fetchReviews();
});