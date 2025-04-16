// Trong users.js
$(document).ready(function () {
    let currentPage = 1;
    const pageSize = 10;

    // Tải danh sách người dùng khi vào trang
    loadUsers(currentPage, '');

    // Xử lý tìm kiếm
    $('#search-users').on('input', function () {
        const searchTerm = $(this).val().trim();
        loadUsers(1, searchTerm);
    });

    // Hàm tải danh sách người dùng
    function loadUsers(page, searchTerm) {
        const url = `/api/quanly/users?page=${page}&size=${pageSize}&search=${encodeURIComponent(searchTerm)}`;
        $.ajax({
            url: url,
            method: 'GET',
            success: function (data) {
                renderUsers(data);
                updatePagination(page, data.length);
            },
            error: function (xhr) {
                console.error('Lỗi tải danh sách người dùng:', xhr);
                Utils.showAlert('error', 'Lỗi', 'Không thể tải danh sách người dùng: ' + (xhr.responseText || 'Unknown error'));
            }
        });
    }

    // Hàm hiển thị danh sách người dùng
    function renderUsers(users) {
        const tbody = $('#users-tbody');
        tbody.empty();

        if (!users || users.length === 0) {
            tbody.append('<tr><td colspan="6" class="text-center">Không tìm thấy người dùng.</td></tr>');
            return;
        }

        users.forEach(user => {
            const row = `
                <tr>
                    <td>${user.id}</td>
                    <td>${user.name || 'N/A'}</td>
                    <td>${user.email || 'N/A'}</td>
                    <td>${user.phoneNumber || 'N/A'}</td>
                    <td>${user.address || 'N/A'}</td>
                    <td>
                        <button class="btn btn-warning btn-sm" data-bs-toggle="modal" 
                                data-bs-target="#userDetailModal" data-user-id="${user.id}">Chi tiết</button>
                        <button class="btn btn-danger btn-sm delete-user" data-user-id="${user.id}">Xóa</button>
                    </td>
                </tr>
            `;
            tbody.append(row);
        });
    }

    // Hàm cập nhật phân trang
    function updatePagination(currentPage, totalItems) {
        const totalPages = Math.ceil(totalItems / pageSize);
        const pagination = $('#users-pagination');
        pagination.empty();

        pagination.append(`
            <li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${currentPage - 1}">Trước</a>
            </li>
        `);

        for (let i = 1; i <= totalPages; i++) {
            pagination.append(`
                <li class="page-item ${i === currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" data-page="${i}">${i}</a>
                </li>
            `);
        }

        pagination.append(`
            <li class="page-item ${currentPage === totalPages ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${currentPage + 1}">Sau</a>
            </li>
        `);
    }

    // Xử lý click phân trang
    $('#users-pagination').on('click', '.page-link', function (e) {
        e.preventDefault();
        const page = $(this).data('page');
        if (page && page > 0) {
            currentPage = page;
            const searchTerm = $('#search-users').val().trim();
            loadUsers(currentPage, searchTerm);
        }
    });

    // Xử lý khi nhấn nút "Chi tiết"
    $('#users-table').on('click', 'button[data-bs-target="#userDetailModal"]', function () {
        const userId = $(this).data('user-id');
        $.ajax({
            url: `/api/quanly/users/${userId}`,
            method: 'GET',
            success: function (user) {
                $('#detail_user_id').val(user.id);
                $('#detail_user_name').val(user.name || '');
                $('#detail_user_email').val(user.email || '');
                $('#detail_user_phone').val(user.phoneNumber || '');
                $('#detail_user_address').val(user.address || '');
                $('#detail_user_dob').val(user.dateOfBirth || '');
                $('#detail_user_role').val(user.role || '');
                if (user.picture) {
                    $('#detail_user_picture').attr('src', user.picture).show();
                } else {
                    $('#detail_user_picture').hide();
                }

                loadUserOrders(user.id);
            },
            error: function (xhr) {
                console.error('Lỗi tải thông tin người dùng:', xhr);
                Utils.showAlert('error', 'Lỗi', 'Không thể tải thông tin người dùng: ' + (xhr.responseText || 'Unknown error'));
            }
        });
    });

    // Hàm tải danh sách đơn hàng
    function loadUserOrders(userId) {
        $.ajax({
            url: `/api/quanly/orders?user_id=${userId}`,
            method: 'GET',
            success: function (orders) {
                const tbody = $('#user_orders_table_body');
                tbody.empty();

                if (!orders || orders.length === 0) {
                    tbody.append('<tr><td colspan="6" class="text-center">Không có đơn hàng nào.</td></tr>');
                    return;
                }

                orders.forEach(order => {
                    const productList = 'Danh sách sản phẩm';
                    const row = `
                        <tr>
                            <td>${order.id}</td>
                            <td>${order.orderDate ? new Date(order.orderDate).toLocaleString('vi-VN') : 'N/A'}</td>
                            <td>${order.totalPrice ? order.totalPrice.toLocaleString('vi-VN') : '0'}</td>
                            <td>${order.status || 'N/A'}</td>
                            <td>${productList}</td>
                            <td>
                                <button class="btn btn-info btn-sm view-order-details" data-order-id="${order.id}">Xem</button>
                            </td>
                        </tr>
                    `;
                    tbody.append(row);
                });
            },
            error: function (xhr) {
                console.error('Lỗi tải danh sách đơn hàng:', xhr);
                Utils.showAlert('error', 'Lỗi', 'Không thể tải danh sách đơn hàng: ' + (xhr.responseText || 'Unknown error'));
            }
        });
    }

    // Xử lý nút "Xóa"
    $('#users-table').on('click', '.delete-user', function () {
        const userId = $(this).data('user-id');
        Utils.confirmAction('Xác nhận xóa', 'Bạn có chắc muốn xóa người dùng này?', function () {
            $.ajax({
                url: `/api/quanly/users/${userId}`,
                method: 'DELETE',
                success: function () {
                    Utils.showAlert('success', 'Thành công', 'Người dùng đã được xóa!');
                    loadUsers(currentPage, $('#search-users').val().trim());
                },
                error: function (xhr) {
                    console.error('Lỗi xóa người dùng:', xhr);
                    Utils.showAlert('error', 'Lỗi', 'Không thể xóa người dùng: ' + (xhr.responseText || 'Unknown error'));
                }
            });
        });
    });

    // Xử lý nút "Chỉnh sửa"
    $('#editUserBtn').click(function () {
        $('#userDetailForm input, #userDetailForm select').not('#detail_user_id').prop('readonly', false).prop('disabled', false);
        $('#editUserBtn').hide();
        $('#saveUserChanges').show();
    });

    // Xử lý nút "Lưu thay đổi"
    $('#saveUserChanges').click(function () {
        let dob = $('#detail_user_dob').val();
        if (dob) {
            const parts = dob.split('/');
            if (parts.length === 3) {
                dob = `${parts[2]}-${parts[1]}-${parts[0]}`; // Chuyển từ DD/MM/YYYY sang YYYY-MM-DD
            }
        }

        const userData = {
            id: $('#detail_user_id').val(),
            name: $('#detail_user_name').val(),
            email: $('#detail_user_email').val(),
            phoneNumber: $('#detail_user_phone').val(),
            address: $('#detail_user_address').val(),
            dateOfBirth: dob || null,
            role: $('#detail_user_role').val(),
            picture: $('#detail_user_picture').attr('src') || null
        };

        $.ajax({
            url: `/api/quanly/users/${userData.id}`,
            method: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify(userData),
            success: function () {
                Utils.showAlert('success', 'Thành công', 'Thông tin người dùng đã được cập nhật!');
                $('#userDetailForm input, #userDetailForm select').prop('readonly', true).prop('disabled', true);
                $('#editUserBtn').show();
                $('#saveUserChanges').hide();
                loadUsers(currentPage, $('#search-users').val().trim());
            },
            error: function (xhr) {
                console.error('Lỗi cập nhật người dùng:', xhr);
                Utils.showAlert('error', 'Lỗi', 'Không thể cập nhật người dùng: ' + (xhr.responseText || 'Unknown error'));
            }
        });
    });

    // Xử lý nút "Xem" chi tiết đơn hàng
    $('#user_orders_table').on('click', '.view-order-details', function (e) {
        e.preventDefault(); // Ngăn chặn hành vi mặc định
        console.log('Nút Xem được nhấn'); // Debug
        const orderId = $(this).data('order-id');
        console.log('Order ID:', orderId); // Debug

        $.ajax({
            url: `/api/quanly/orders/${orderId}`,
            method: 'GET',
            success: function (order) {
                console.log('Dữ liệu đơn hàng:', order); // Debug
                // Gán thông tin đơn hàng vào modal
                $('#order_detail_id').text(order.id || 'N/A');
                $('#order_detail_date').text(order.orderDate ? new Date(order.orderDate).toLocaleString('vi-VN') : 'N/A');
                $('#order_detail_total').text(order.totalPrice ? order.totalPrice.toLocaleString('vi-VN') : '0');
                $('#order_detail_status').text(order.status || 'N/A');

                // Tải danh sách sản phẩm trong đơn hàng
                loadOrderItems(order.id);

                // Mở modal
                try {
                    $('#orderDetailModal').modal('show');
                    console.log('Modal được mở'); // Debug
                } catch (err) {
                    console.error('Lỗi khi mở modal:', err);
                    Utils.showAlert('error', 'Lỗi', 'Không thể mở modal chi tiết đơn hàng.');
                }
            },
            error: function (xhr) {
                console.error('Lỗi tải thông tin đơn hàng:', xhr);
                Utils.showAlert('error', 'Lỗi', 'Không thể tải thông tin đơn hàng: ' + (xhr.responseText || 'Unknown error'));
            }
        });
    });

    function loadOrderItems(orderId) {
        console.log('Tải danh sách sản phẩm cho orderId:', orderId); // Debug
        const tbody = $('#order_items_table_body');
        tbody.html('<tr><td colspan="6" class="text-center">Đang tải...</td></tr>');

        $.ajax({
            url: `/api/quanly/orders/${orderId}/items`,
            method: 'GET',
            success: function (response) {
                console.log('Phản hồi API:', response); // Debug chi tiết
                tbody.empty();

                // Kiểm tra nếu response không phải là mảng hoặc rỗng
                if (!response || !Array.isArray(response) || response.length === 0) {
                    console.log('Dữ liệu rỗng hoặc không phải mảng:', response);
                    tbody.append('<tr><td colspan="6" class="text-center">Không có sản phẩm nào trong đơn hàng.</td></tr>');
                    return;
                }

                // Duyệt qua danh sách sản phẩm
                response.forEach(item => {
                    const row = `
                    <tr>
                        <td>${item.id || 'N/A'}</td>
                        <td>${item.productName || 'N/A'}</td>
                        <td>${item.donViTinh || 'N/A'}</td>
                        <td>${item.quantity || 0}</td>
                        <td>${item.price ? item.price.toLocaleString('vi-VN') : '0'}</td>
                        <td>${item.totalPrice ? item.totalPrice.toLocaleString('vi-VN') : '0'}</td>
                    </tr>
                `;
                    tbody.append(row);
                });
            },
            error: function (xhr) {
                console.error('Lỗi tải danh sách sản phẩm:', xhr.responseText); // Ghi lỗi chi tiết
                Utils.showAlert('error', 'Lỗi', 'Không thể tải danh sách sản phẩm: ' + (xhr.responseText || 'Unknown error'));
                tbody.empty().append('<tr><td colspan="6" class="text-center">Không thể tải sản phẩm.</td></tr>');
            }
        });
    }


});