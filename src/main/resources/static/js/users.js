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
                Utils.showAlert('error', 'Lỗi', 'Không thể tải danh sách người dùng: ' + xhr.responseText);
            }
        });
    }

    // Hàm hiển thị danh sách người dùng
    function renderUsers(users) {
        const tbody = $('#users-tbody');
        tbody.empty();

        if (users.length === 0) {
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

        // Nút Previous
        pagination.append(`
            <li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${currentPage - 1}">Trước</a>
            </li>
        `);

        // Các trang
        for (let i = 1; i <= totalPages; i++) {
            pagination.append(`
                <li class="page-item ${i === currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" data-page="${i}">${i}</a>
                </li>
            `);
        }

        // Nút Next
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
                // Gán dữ liệu vào modal
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

                // Tải danh sách đơn hàng
                loadUserOrders(user.id);
            },
            error: function (xhr) {
                Utils.showAlert('error', 'Lỗi', 'Không thể tải thông tin người dùng: ' + xhr.responseText);
            }
        });
    });

    // Hàm tải danh sách đơn hàng
// Hàm tải danh sách đơn hàng
    function loadUserOrders(userId) {
        $.ajax({
            url: `/api/quanly/orders?user_id=${userId}`,
            method: 'GET',
            success: function (orders) {
                const tbody = $('#user_orders_table_body');
                tbody.empty();

                if (orders.length === 0) {
                    tbody.append('<tr><td colspan="6" class="text-center">Không có đơn hàng nào.</td></tr>');
                    return;
                }

                orders.forEach(order => {
                    const productList = 'Danh sách sản phẩm'; // Thay bằng API lấy chi tiết đơn hàng
                    const row = `
                    <tr>
                        <td>${order.id}</td>
                        <td>${new Date(order.orderDate).toLocaleString('vi-VN')}</td>
                        <td>${order.totalPrice.toLocaleString('vi-VN')}</td>
                        <td>${order.status}</td>
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
                Utils.showAlert('error', 'Lỗi', 'Không thể tải danh sách đơn hàng: ' + xhr.responseText);
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
                    Utils.showAlert('error', 'Lỗi', 'Không thể xóa người dùng: ' + xhr.responseText);
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
        // Chuyển đổi dateOfBirth sang định dạng YYYY-MM-DD nếu cần
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
                Utils.showAlert('error', 'Lỗi', 'Không thể cập nhật người dùng: ' + xhr.responseText);
            }
        });
    });


    // Xử lý nút "Xem" chi tiết đơn hàng
// Xử lý nút "Xem" chi tiết đơn hàng
    $('#user_orders_table').on('click', '.view-order-details', function () {
        const orderId = $(this).data('order-id');
        $.ajax({
            url: `/api/quanly/orders/${orderId}`,
            method: 'GET',
            success: function (order) {
                // Gán thông tin đơn hàng vào modal
                $('#order_detail_id').text(order.id);
                $('#order_detail_date').text(new Date(order.orderDate).toLocaleString('vi-VN'));
                $('#order_detail_total').text(order.totalPrice.toLocaleString('vi-VN'));
                $('#order_detail_status').text(order.status);

                // Tải danh sách sản phẩm trong đơn hàng
                loadOrderItems(order.id);

                // Mở modal
                $('#orderDetailModal').modal('show');
            },
            error: function (xhr) {
                Utils.showAlert('error', 'Lỗi', 'Không thể tải thông tin đơn hàng: ' + xhr.responseText);
            }
        });
    });

// Hàm tải danh sách sản phẩm trong đơn hàng (cart_items)
    function loadOrderItems(orderId) {
        $.ajax({
            url: `/api/quanly/orders/${orderId}/items`,
            method: 'GET',
            success: function (items) {
                const tbody = $('#order_items_table_body');
                tbody.empty();

                if (items.length === 0) {
                    tbody.append('<tr><td colspan="6" class="text-center">Không có sản phẩm nào trong đơn hàng.</td></tr>');
                    return;
                }

                items.forEach(item => {
                    const row = `
                    <tr>
                        <td>${item.id}</td>
                        <td>${item.productName || 'N/A'}</td>
                        <td>${item.donViTinh || 'N/A'}</td>
                        <td>${item.quantity}</td>
                        <td>${item.price.toLocaleString('vi-VN')}</td>
                        <td>${item.totalPrice.toLocaleString('vi-VN')}</td>
                    </tr>
                `;
                    tbody.append(row);
                });
            },
            error: function (xhr) {
                Utils.showAlert('error', 'Lỗi', 'Không thể tải danh sách sản phẩm: ' + xhr.responseText);
            }
        });
    }




});