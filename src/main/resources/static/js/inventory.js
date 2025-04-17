class InventoryManager {
    constructor() {
        this.init();
    }

    init() {
        // Khởi tạo các sự kiện
        $('.nav-link[href="#inventory"]').on('click', () => {
            this.loadInventory();
            this.loadTransactions();
            this.loadFilterWarehouses();
            this.loadFilterProducts();
            this.loadFilterSuppliers();
        });

        // Sự kiện tìm kiếm và lọc
        $('#search-inventory, #filter-warehouse, #filter-product, #filter-expiry-status, #filter-supplier')
            .on('change keyup', () => this.loadInventory());

        $('#search-transactions, #filter-transaction-type, #filter-transaction-date')
            .on('change keyup', () => this.loadTransactions());

        // Sự kiện nút trong modal
        $('#saveInventory').on('click', () => this.saveInventory());
        $('#saveEditInventory').on('click', () => this.saveEditInventory());
        $('#exportInventoryPDF').on('click', () => this.exportPDF());

        // Load dữ liệu khi mở modal
        $('#addInventoryModal').on('show.bs.modal', () => {
            this.loadWarehousesForModal();
            this.loadProductsForModal();
            this.loadSuppliersForModal();
        });

        $('#editInventoryModal').on('show.bs.modal', () => {
            this.loadFilterSuppliers();
        });

        // Load kho khi mở modal xem kho
        $('#viewWarehouseModal').on('show.bs.modal', () => this.loadFilterWarehouses());
        $('#saveWarehouse').on('click', () => this.saveWarehouse());
        $('#saveEditWarehouse').on('click', () => this.saveEditWarehouse());

        // Sự kiện tìm kiếm và lọc kho
        $('#search-warehouse, #filter-warehouse-type, #filter-warehouse-status')
            .on('input change', () => this.loadFilterWarehouses());

        // Sự kiện sắp xếp cột kho
        $('#warehouse-table .sort-icon').on('click', (e) => this.sortWarehouseTable(e));
    }

    // Load danh sách tồn kho
    loadInventory(page = 1) {
        const search = $('#search-inventory').val().toLowerCase();
        const warehouseFilter = $('#filter-warehouse').val();
        const productFilter = $('#filter-product').val();
        const expiryStatus = $('#filter-expiry-status').val();
        const supplierFilter = $('#filter-supplier').val();
        const pageSize = 10;

        $.ajax({
            url: '/api/quanly/inventory',
            method: 'GET',
            success: (data) => {
                if (!Array.isArray(data)) {
                    Swal.fire({ icon: 'error', title: 'Lỗi', text: 'Dữ liệu không hợp lệ!' });
                    return;
                }

                let filteredData = data.filter(item => {
                    const matchesSearch = (item.product?.tenSanPham?.toLowerCase() || '').includes(search) ||
                        (item.batchNumber?.toLowerCase() || '').includes(search) ||
                        (item.kho?.maKho?.toLowerCase() || '').includes(search);
                    const matchesWarehouse = !warehouseFilter || item.kho?.id == warehouseFilter;
                    const matchesProduct = !productFilter || item.product?.id == productFilter;
                    const matchesSupplier = !supplierFilter || (item.supplier && item.supplier.id == supplierFilter);
                    let matchesExpiry = true;
                    if (expiryStatus && item.expiryDate) {
                        const expiryDateObj = new Date(item.expiryDate);
                        const today = new Date();
                        const daysDiff = (expiryDateObj - today) / (1000 * 60 * 60 * 24);
                        if (expiryStatus === 'near-expiry') matchesExpiry = daysDiff <= 30 && daysDiff >= 0;
                        else if (expiryStatus === 'expired') matchesExpiry = daysDiff < 0;
                        else if (expiryStatus === 'valid') matchesExpiry = daysDiff > 30;
                    }
                    return matchesSearch && matchesWarehouse && matchesProduct && matchesSupplier && matchesExpiry;
                });

                this.renderInventory(filteredData, page, pageSize);
            },
            error: () => Swal.fire({ icon: 'error', title: 'Lỗi', text: 'Không thể tải danh sách tồn kho!' })
        });
    }

    // Hiển thị danh sách tồn kho
    renderInventory(data, page, pageSize) {
        const tbody = $('#inventory-tbody');
        tbody.empty();
        const totalItems = data.length;
        const totalPages = Math.ceil(totalItems / pageSize);
        const start = (page - 1) * pageSize;
        const end = Math.min(start + pageSize, totalItems);
        const paginated = data.slice(start, end);

        paginated.forEach(item => {
            const expiryDateObj = item.expiryDate ? new Date(item.expiryDate) : null;
            const today = new Date();
            const daysDiff = expiryDateObj ? (expiryDateObj - today) / (1000 * 60 * 60 * 24) : null;
            let status = daysDiff !== null ? (daysDiff < 0 ? 'Đã hết hạn' : (daysDiff <= 30 ? 'Sắp hết hạn' : 'Còn hạn')) : 'N/A';
            let statusClass = daysDiff !== null ? (daysDiff < 0 ? 'inventory-status-expired' : (daysDiff <= 30 ? 'inventory-status-near-expiry' : 'inventory-status-valid')) : '';

            const row = `
                <tr>
                    <td>${item.id || 'N/A'}</td>
                    <td>${item.kho ? `${item.kho.maKho} - ${item.kho.tenKho}` : 'N/A'}</td>
                    <td>${item.product?.tenSanPham || 'N/A'}</td>
                    <td>${item.batchNumber || 'N/A'}</td>
                    <td>${item.quantity || 'N/A'}</td>
                    <td>${item.supplier ? item.supplier.tenNhaCungCap : 'N/A'}</td>
                    <td>${item.manufactureDate ? item.manufactureDate.split('T')[0] : 'N/A'}</td>
                    <td>${item.expiryDate ? item.expiryDate.split('T')[0] : 'N/A'}</td>
                    <td>${item.storageLocation || 'N/A'}</td>
                    <td class="${statusClass}">${status}</td>
                    <td>
                        <button class="btn btn-warning btn-sm edit-inventory" data-id="${item.id}" data-bs-toggle="modal" data-bs-target="#editInventoryModal">Sửa</button>
                        <button class="btn btn-danger btn-sm delete-inventory" data-id="${item.id}">Xóa</button>
                    </td>
                </tr>
            `;
            tbody.append(row);
        });

        this.bindInventoryEvents(data);
        this.renderPagination('#inventory-pagination', totalPages, page, (selectedPage) => this.loadInventory(selectedPage));
    }

    // Bind sự kiện cho các nút trong bảng tồn kho
    bindInventoryEvents(data) {
        $('.edit-inventory').off('click').on('click', (e) => {
            const id = $(e.target).data('id');
            const item = data.find(i => i.id === id);
            $('#edit_inventory_id').val(item.id);
            $('#edit_inventory_warehouse_id').val(item.kho?.id || '');
            $('#edit_inventory_product_id').val(item.product?.id || '');
            $('#edit_inventory_batch_number').val(item.batchNumber || '');
            $('#edit_inventory_quantity').val(item.quantity || '');
            $('#edit_inventory_supplier_id').val(item.supplier ? item.supplier.id : '');
            $('#edit_inventory_manufacture_date').val(item.manufactureDate ? item.manufactureDate.split('T')[0] : '');
            $('#edit_inventory_expiry_date').val(item.expiryDate ? item.expiryDate.split('T')[0] : '');
            $('#edit_inventory_storage_location').val(item.storageLocation || '');
        });

        $('.delete-inventory').off('click').on('click', (e) => {
            const id = $(e.target).data('id');
            Swal.fire({
                title: 'Xác nhận xóa',
                text: `Bạn có chắc chắn muốn xóa lô hàng ID ${id}?`,
                icon: 'warning',
                showCancelButton: true,
                confirmButtonText: 'Có, xóa!',
                cancelButtonText: 'Hủy'
            }).then((result) => {
                if (result.isConfirmed) {
                    $.ajax({
                        url: `/api/quanly/inventory/${id}`,
                        method: 'DELETE',
                        success: () => {
                            Swal.fire({ icon: 'success', title: 'Thành công', text: 'Lô hàng đã được xóa!', timer: 1500 });
                            this.loadInventory();
                        },
                        error: () => Swal.fire({ icon: 'error', title: 'Lỗi', text: 'Không thể xóa lô hàng!' })
                    });
                }
            });
        });
    }

    // Lưu lô hàng mới
    saveInventory() {
        const inventoryData = {
            warehouseId: parseInt($('#inventory_warehouse_id').val()),
            productId: parseInt($('#inventory_product_id').val()),
            batchNumber: $('#inventory_batch_number').val(),
            quantity: parseInt($('#inventory_quantity').val()),
            supplierId: $('#inventory_supplier_id').val() ? parseInt($('#inventory_supplier_id').val()) : null,
            manufactureDate: $('#inventory_manufacture_date').val() + "T00:00:00",
            expiryDate: $('#inventory_expiry_date').val() + "T00:00:00",
            storageLocation: $('#inventory_storage_location').val() || null,
            note: $('#inventory_note').val() || null
        };

        if (!inventoryData.warehouseId || !inventoryData.productId || !inventoryData.batchNumber ||
            !inventoryData.quantity || !inventoryData.manufactureDate || !inventoryData.expiryDate) {
            Swal.fire({ icon: 'warning', title: 'Cảnh báo', text: 'Vui lòng điền đầy đủ các trường bắt buộc!' });
            return;
        }

        $.ajax({
            url: '/api/quanly/inventory',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(inventoryData),
            success: (response) => {
                if (response && response.id) {
                    Swal.fire({ icon: 'success', title: 'Thành công', text: 'Lô hàng đã được thêm!', timer: 1500 });
                    $('#addInventoryModal').modal('hide');
                    $('#addInventoryForm')[0].reset();
                    this.loadInventory();
                } else {
                    Swal.fire({ icon: 'error', title: 'Lỗi', text: 'Thêm lô hàng thất bại!' });
                }
            },
            error: (xhr) => Swal.fire({ icon: 'error', title: 'Lỗi', text: xhr.responseText || 'Không thể thêm lô hàng!' })
        });
    }

    // Lưu thông tin lô hàng đã chỉnh sửa
    saveEditInventory() {
        const inventoryData = {
            id: parseInt($('#edit_inventory_id').val()),
            warehouseId: parseInt($('#edit_inventory_warehouse_id').val()),
            productId: parseInt($('#edit_inventory_product_id').val()),
            batchNumber: $('#edit_inventory_batch_number').val(),
            quantity: parseInt($('#edit_inventory_quantity').val()),
            supplierId: $('#edit_inventory_supplier_id').val() ? parseInt($('#edit_inventory_supplier_id').val()) : null,
            manufactureDate: $('#edit_inventory_manufacture_date').val(),
            expiryDate: $('#edit_inventory_expiry_date').val(),
            storageLocation: $('#edit_inventory_storage_location').val() || null
        };

        if (!inventoryData.warehouseId || !inventoryData.productId || !inventoryData.batchNumber ||
            !inventoryData.quantity || !inventoryData.manufactureDate || !inventoryData.expiryDate) {
            Swal.fire({ icon: 'warning', title: 'Cảnh báo', text: 'Vui lòng điền đầy đủ các trường bắt buộc!' });
            return;
        }

        $.ajax({
            url: `/api/quanly/inventory/${inventoryData.id}`,
            method: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify(inventoryData),
            success: () => {
                Swal.fire({ icon: 'success', title: 'Thành công', text: 'Lô hàng đã được cập nhật!', timer: 1500 });
                $('#editInventoryModal').modal('hide');
                this.loadInventory();
            },
            error: () => Swal.fire({ icon: 'error', title: 'Lỗi', text: 'Không thể cập nhật lô hàng!' })
        });
    }

    // Load lịch sử giao dịch
    loadTransactions(page = 1) {
        const search = $('#search-transactions').val().toLowerCase();
        const typeFilter = $('#filter-transaction-type').val();
        const dateFilter = $('#filter-transaction-date').val();
        const pageSize = 10;

        $.ajax({
            url: '/api/quanly/inventory-transactions',
            method: 'GET',
            success: (data) => {
                if (!Array.isArray(data)) {
                    Swal.fire({ icon: 'error', title: 'Lỗi', text: 'Dữ liệu không hợp lệ!' });
                    return;
                }

                let filteredData = data.filter(item => {
                    const matchesSearch = (item.product?.tenSanPham?.toLowerCase() || '').includes(search) ||
                        (item.batchNumber?.toLowerCase() || '').includes(search) ||
                        (item.transactionType?.toLowerCase() || '').includes(search);
                    const matchesType = !typeFilter || item.transactionType === typeFilter;
                    const matchesDate = !dateFilter || (item.transactionDate && item.transactionDate.split('T')[0] === dateFilter);
                    return matchesSearch && matchesType && matchesDate;
                });

                this.renderTransactions(filteredData, page, pageSize);
            },
            error: (xhr) => Swal.fire({ icon: 'error', title: 'Lỗi', text: xhr.responseText || 'Không thể tải lịch sử giao dịch!' })
        });
    }
    // Hiển thị lịch sử giao dịch
    renderTransactions(data, page, pageSize) {
        const tbody = $('#transaction-tbody');
        tbody.empty();
        const totalItems = data.length;
        const totalPages = Math.ceil(totalItems / pageSize);
        const start = (page - 1) * pageSize;
        const end = Math.min(start + pageSize, totalItems);
        const paginated = data.slice(start, end);

        const typeMap = {
            'NHAP_KHO': 'Nhập kho',
            'XUAT_KHO': 'Xuất kho',
            'DIEU_CHINH': 'Điều chỉnh'
        };

        paginated.forEach(item => {
            const transactionType = typeMap[item.transactionType] || item.transactionType || 'N/A';
            const row = `
            <tr>
                <td>${item.id || 'N/A'}</td>
                <td>${transactionType}</td>
                <td>${item.product?.tenSanPham || 'N/A'}</td>
                <td>${item.batchNumber || 'N/A'}</td>
                <td>${item.quantity || 'N/A'}</td>
                <td>${item.supplier?.tenNhaCungCap || 'N/A'}</td>
                <td>${item.createdBy?.name || 'N/A'}</td>
                <td>${item.order?.id ? `#${item.order.id}` : 'N/A'}</td>
                <td>${item.transactionDate ? item.transactionDate.split('T')[0] : 'N/A'}</td>
                <td>${item.reason || 'N/A'}</td>
            </tr>
        `;
            tbody.append(row);
        });

        this.renderPagination('#transaction-pagination', totalPages, page, (selectedPage) => this.loadTransactions(selectedPage));
    }
    // Load danh sách kho cho modal
    loadWarehousesForModal() {
        $.ajax({
            url: '/api/quanly/kho',
            method: 'GET',
            success: (warehouses) => {
                const select = $('#inventory_warehouse_id');
                select.find('option:not(:first)').remove();
                warehouses.forEach(w => select.append(`<option value="${w.id}">${w.maKho} - ${w.tenKho}</option>`));
            },
            error: () => Swal.fire({ icon: 'error', title: 'Lỗi', text: 'Không thể tải danh sách kho!' })
        });
    }

    // Load danh sách sản phẩm cho modal
    loadProductsForModal() {
        $.ajax({
            url: '/api/quanly/products',
            method: 'GET',
            success: (products) => {
                const select = $('#inventory_product_id');
                select.find('option:not(:first)').remove();
                products.forEach(p => select.append(`<option value="${p.id}">${p.tenSanPham}</option>`));
            },
            error: () => Swal.fire({ icon: 'error', title: 'Lỗi', text: 'Không thể tải danh sách sản phẩm!' })
        });
    }

    // Load danh sách nhà cung cấp cho modal
    loadSuppliersForModal() {
        $.ajax({
            url: '/api/quanly/nhacungcap',
            method: 'GET',
            success: (suppliers) => {
                const select = $('#inventory_supplier_id');
                select.find('option:not(:first)').remove();
                suppliers.forEach(s => select.append(`<option value="${s.id}">${s.tenNhaCungCap}</option>`));
            },
            error: () => Swal.fire({ icon: 'error', title: 'Lỗi', text: 'Không thể tải danh sách nhà cung cấp!' })
        });
    }

    // Load danh sách kho cho bộ lọc
    loadFilterWarehouses() {
        $.ajax({
            url: '/api/quanly/kho',
            method: 'GET',
            success: (warehouses) => {
                if (!Array.isArray(warehouses)) warehouses = [];
                const selects = $('#filter-warehouse, #inventory_warehouse_id, #edit_inventory_warehouse_id');
                selects.find('option:not(:first)').remove();
                warehouses.forEach(w => selects.append(`<option value="${w.id}">${w.maKho} - ${w.tenKho}</option>`));
                this.renderWarehouseTable(warehouses);
            },
            error: () => Swal.fire({ icon: 'error', title: 'Lỗi', text: 'Không thể tải danh sách kho!' })
        });
    }

    // Load danh sách sản phẩm cho bộ lọc
    loadFilterProducts() {
        $.ajax({
            url: '/api/quanly/products',
            method: 'GET',
            success: (products) => {
                const selects = $('#filter-product, #inventory_product_id, #edit_inventory_product_id');
                selects.find('option:not(:first)').remove();
                products.forEach(p => selects.append(`<option value="${p.id}">${p.tenSanPham}</option>`));
            },
            error: () => Swal.fire({ icon: 'error', title: 'Lỗi', text: 'Không thể tải danh sách sản phẩm!' })
        });
    }

    // Load danh sách nhà cung cấp cho bộ lọc
    loadFilterSuppliers() {
        $.ajax({
            url: '/api/quanly/nhacungcap',
            method: 'GET',
            success: (suppliers) => {
                const selects = $('#filter-supplier, #inventory_supplier_id, #edit_inventory_supplier_id');
                selects.find('option:not(:first)').remove();
                suppliers.forEach(s => selects.append(`<option value="${s.id}">${s.tenNhaCungCap}</option>`));
            },
            error: () => Swal.fire({ icon: 'error', title: 'Lỗi', text: 'Không thể tải danh sách nhà cung cấp!' })
        });
    }

    // Hiển thị bảng kho
    renderWarehouseTable(warehouses) {
        const search = $('#search-warehouse').val().toLowerCase();
        const typeFilter = $('#filter-warehouse-type').val();
        const statusFilter = $('#filter-warehouse-status').val();

        let filteredWarehouses = warehouses.filter(w => {
            const matchesSearch = (w.maKho?.toLowerCase() || '').includes(search) || (w.tenKho?.toLowerCase() || '').includes(search);
            const matchesType = !typeFilter || w.loaiKho === typeFilter;
            const matchesStatus = !statusFilter || w.trangThai === statusFilter;
            return matchesSearch && matchesType && matchesStatus;
        });

        const sortColumn = $('#warehouse-table .sort-icon.active').data('column');
        const sortDirection = $('#warehouse-table .sort-icon.active').hasClass('fa-sort-up') ? 1 : -1;
        if (sortColumn) {
            filteredWarehouses.sort((a, b) => {
                let valA = a[sortColumn] || 0;
                let valB = b[sortColumn] || 0;
                return (valA - valB) * sortDirection;
            });
        }

        const tbody = $('#warehouse-tbody');
        tbody.empty();

        const statusMap = { 'ACTIVE': 'Hoạt động', 'INACTIVE': 'Ngừng hoạt động', 'MAINTENANCE': 'Bảo trì' };

        filteredWarehouses.forEach(w => {
            const typeBadgeClass = w.loaiKho === 'THƯỜNG' ? 'badge-success' : w.loaiKho === 'LẠNH' ? 'badge-danger' : 'badge-warning';
            const statusBadgeClass = w.trangThai === 'ACTIVE' ? 'badge-success' : w.trangThai === 'INACTIVE' ? 'badge-danger' : 'badge-warning';
            const statusDisplay = statusMap[w.trangThai] || w.trangThai;

            const row = `
                <tr data-id="${w.id}">
                    <td>${w.id}</td>
                    <td>${w.maKho || 'N/A'}</td>
                    <td>${w.tenKho || 'N/A'}</td>
                    <td>${w.diaChi || 'N/A'}</td>
                    <td>${w.dienTich ? w.dienTich.toFixed(2) : 'N/A'}</td>
                    <td>${w.sucChua || 'N/A'}</td>
                    <td><span class="badge ${typeBadgeClass}">${w.loaiKho || 'N/A'}</span></td>
                    <td><span class="badge ${statusBadgeClass}">${statusDisplay}</span></td>
                    <td>${w.nguoiQuanly || 'N/A'}</td>
                    <td>
                        <button class="btn btn-warning btn-sm edit-warehouse" data-id="${w.id}" data-bs-toggle="modal" data-bs-target="#editWarehouseModal"><i class="fas fa-edit"></i></button>
                        <button class="btn btn-danger btn-sm delete-warehouse" data-id="${w.id}"><i class="fas fa-trash"></i></button>
                    </td>
                </tr>
            `;
            tbody.append(row);
        });

        this.bindWarehouseEvents(warehouses);
    }

    // Bind sự kiện cho bảng kho
    bindWarehouseEvents(warehouses) {
        $('.edit-warehouse').off('click').on('click', (e) => {
            const id = $(e.target).data('id') || $(e.target).parent().data('id');
            const warehouse = warehouses.find(w => w.id === id);
            $('#edit_warehouse_id').val(warehouse.id);
            $('#edit_warehouse_ma_kho').val(warehouse.maKho);
            $('#edit_warehouse_ten_kho').val(warehouse.tenKho);
            $('#edit_warehouse_dia_chi').val(warehouse.diaChi);
            $('#edit_warehouse_dien_tich').val(warehouse.dienTich);
            $('#edit_warehouse_suc_chua').val(warehouse.sucChua);
            $('#edit_warehouse_loai_kho').val(warehouse.loaiKho);
            $('#edit_warehouse_trang_thai').val(warehouse.trangThai);
            $('#edit_warehouse_so_dien_thoai').val(warehouse.soDienThoaiLienHe);
            $('#edit_warehouse_email').val(warehouse.emailLienHe);
            $('#edit_warehouse_nguoi_quan_ly').val(warehouse.nguoiQuanly);
            $('#edit_warehouse_ghi_chu').val(warehouse.ghiChu);
        });

        $('.delete-warehouse').off('click').on('click', (e) => {
            const id = $(e.target).data('id') || $(e.target).parent().data('id');
            Swal.fire({
                title: 'Xác nhận xóa',
                text: `Bạn có chắc chắn muốn xóa kho ID ${id}?`,
                icon: 'warning',
                showCancelButton: true,
                confirmButtonText: 'Có, xóa!',
                cancelButtonText: 'Hủy'
            }).then((result) => {
                if (result.isConfirmed) {
                    $.ajax({
                        url: `/api/quanly/kho/${id}`,
                        method: 'DELETE',
                        success: () => {
                            Swal.fire({ icon: 'success', title: 'Thành công', text: 'Kho đã được xóa!', timer: 1500 });
                            this.loadFilterWarehouses();
                        },
                        error: () => Swal.fire({ icon: 'error', title: 'Lỗi', text: 'Không thể xóa kho!' })
                    });
                }
            });
        });
    }

    // Lưu kho mới
    saveWarehouse() {
        const warehouseData = {
            maKho: $('#warehouse_ma_kho').val(),
            tenKho: $('#warehouse_ten_kho').val(),
            diaChi: $('#warehouse_dia_chi').val() || null,
            dienTich: parseFloat($('#warehouse_dien_tich').val()) || null,
            sucChua: parseInt($('#warehouse_suc_chua').val()) || null,
            loaiKho: $('#warehouse_loai_kho').val(),
            trangThai: $('#warehouse_trang_thai').val(),
            soDienThoaiLienHe: $('#warehouse_so_dien_thoai').val() || null,
            emailLienHe: $('#warehouse_email').val() || null,
            nguoiQuanly: $('#warehouse_nguoi_quan_ly').val() || null,
            ghiChu: $('#warehouse_ghi_chu').val() || null
        };

        if (!warehouseData.maKho || !warehouseData.tenKho || !warehouseData.loaiKho || !warehouseData.trangThai) {
            Swal.fire({ icon: 'warning', title: 'Cảnh báo', text: 'Vui lòng điền đầy đủ các trường bắt buộc!' });
            return;
        }

        $.ajax({
            url: '/api/quanly/kho',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(warehouseData),
            success: () => {
                Swal.fire({ icon: 'success', title: 'Thành công', text: 'Kho mới đã được thêm!', timer: 1500 });
                $('#addWarehouseModal').modal('hide');
                $('#addWarehouseForm')[0].reset();
                this.loadFilterWarehouses();
            },
            error: (xhr) => Swal.fire({ icon: 'error', title: 'Lỗi', text: xhr.responseText || 'Không thể thêm kho!' })
        });
    }

    // Lưu thông tin kho đã chỉnh sửa
    saveEditWarehouse() {
        const warehouseData = {
            id: parseInt($('#edit_warehouse_id').val()),
            maKho: $('#edit_warehouse_ma_kho').val(),
            tenKho: $('#edit_warehouse_ten_kho').val(),
            diaChi: $('#edit_warehouse_dia_chi').val() || null,
            dienTich: parseFloat($('#edit_warehouse_dien_tich').val()) || null,
            sucChua: parseInt($('#edit_warehouse_suc_chua').val()) || null,
            loaiKho: $('#edit_warehouse_loai_kho').val(),
            trangThai: $('#edit_warehouse_trang_thai').val(),
            soDienThoaiLienHe: $('#edit_warehouse_so_dien_thoai').val() || null,
            emailLienHe: $('#edit_warehouse_email').val() || null,
            nguoiQuanly: $('#edit_warehouse_nguoi_quan_ly').val() || null,
            ghiChu: $('#edit_warehouse_ghi_chu').val() || null
        };

        if (!warehouseData.maKho || !warehouseData.tenKho || !warehouseData.loaiKho || !warehouseData.trangThai) {
            Swal.fire({ icon: 'warning', title: 'Cảnh báo', text: 'Vui lòng điền đầy đủ các trường bắt buộc!' });
            return;
        }

        $.ajax({
            url: `/api/quanly/kho/${warehouseData.id}`,
            method: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify(warehouseData),
            success: () => {
                Swal.fire({ icon: 'success', title: 'Thành công', text: 'Kho đã được cập nhật!', timer: 1500 });
                $('#editWarehouseModal').modal('hide');
                this.loadFilterWarehouses();
            },
            error: () => Swal.fire({ icon: 'error', title: 'Lỗi', text: 'Không thể cập nhật kho!' })
        });
    }

    // Xuất PDF (giả lập)
    exportPDF() {
        Swal.fire({ icon: 'info', title: 'Xuất PDF', text: 'Chức năng xuất PDF đang được phát triển!' });
    }

    // Sắp xếp bảng kho
    sortWarehouseTable(e) {
        const column = $(e.target).data('column');
        const isAscending = $(e.target).hasClass('fa-sort-up');

        $('#warehouse-table .sort-icon').removeClass('fa-sort-up fa-sort-down active').addClass('fa-sort');
        $(e.target).removeClass('fa-sort').addClass(isAscending ? 'fa-sort-down' : 'fa-sort-up').addClass('active');

        this.loadFilterWarehouses();
    }

    // Hiển thị phân trang
    renderPagination(selector, totalPages, currentPage, callback) {
        const pagination = $(selector);
        pagination.empty();
        for (let i = 1; i <= totalPages; i++) {
            const liClass = i === currentPage ? 'page-item active' : 'page-item';
            pagination.append(`<li class="${liClass}"><a class="page-link" href="#" data-page="${i}">${i}</a></li>`);
        }
        $('.page-link').off('click').on('click', (e) => {
            e.preventDefault();
            const selectedPage = $(e.target).data('page');
            callback(selectedPage);
        });
    }
}

$(document).ready(() => new InventoryManager());