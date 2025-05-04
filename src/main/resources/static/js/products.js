// Đảm bảo đã load jQuery và jsPDF trước khi chạy script này

import dejaVuSansBase64 from '../font/DejaVuSansFont.js';


$(document).ready(function () {
    const { jsPDF } = window.jspdf;

    class ProductManager {
        constructor() {
            this.imageList = []; // Danh sách hình ảnh tạm thời
            this.unitDataList = []; // Danh sách đơn vị tính tạm thời
            this.unitList = ['Viên', 'Hộp', 'Chai', 'Gói', 'Ống']; // Danh sách đơn vị tính mặc định
            this.products = []; // Danh sách sản phẩm
            this.pageSize = 10;
            this.currentPage = 1;
            this.init();
        }

        init() {
            this.loadProducts(); // Tải danh sách sản phẩm khi khởi tạo
            Utils.loadCategories('#filter-category', 'Lọc theo danh mục'); // Tải danh mục cho bộ lọc
            this.setupEventListeners(); // Thiết lập các sự kiện
        }

        setupEventListeners() {
            $('#search-products').on('input', () => this.loadProducts());
            $('#filter-category').on('change', () => this.loadProducts());

            $('#add-image').on('click', (e) => this.addImage(e));
            $('#add-unit').on('click', (e) => this.addUnit(e));
            $('#add_detail_image').on('click', (e) => this.addDetailImage(e));
            $('#add_detail_unit').on('click', (e) => this.addDetailUnit(e));
            $('#saveProductBtn').on('click', (e) => this.saveProduct(e));

            $('#addProductModal').on('shown.bs.modal', () => {
                $('#addProductForm')[0].reset();
                this.imageList = [];
                this.unitDataList = [];
                this.updateImageTable();
                this.updateUnitTable();
                Utils.loadCategories('#danhmuc_id', 'Chọn danh mục');
                this.loadUnitList();
                this.loadDoiTuong('#doi_tuong_id');
            });

            $('#unit-name').on('change', (e) => {
                if (e.target.value === 'add-unit') this.showCustomUnitInput();
            });
            $('#saveNewUnit').on('click', () => this.saveNewUnit());

            $('#doi_tuong_id').on('change', (e) => {
                if (e.target.value === 'add-doi-tuong') {
                    this.showAddDoiTuongModal();
                    $('#doi_tuong_id').val('');
                }
            });
            $('#saveDoiTuong').on('click', () => this.saveDoiTuong());

            $('#doi_tuong_image_max').on('input', () => this.previewImage('#doi_tuong_image_max', '#preview_image_max'));
            $('#doi_tuong_image_min').on('input', () => this.previewImage('#doi_tuong_image_min', '#preview_image_min'));
            $('#image-url').on('input', () => this.previewImage('#image-url', '#preview-image'));
            $('#detail_image_url').on('input', () => this.previewImage('#detail_image_url', '#detail_preview_image'));
            $('#addDoiTuongModal').on('hidden.bs.modal', () => {
                $('#preview_image_max').hide().attr('src', '');
                $('#preview_image_min').hide().attr('src', '');
            });

            $(document).on('click', '.view-product', (e) => {
                const productId = $(e.target).data('id');
                this.showProductDetail(productId);
            });

            $(document).on('click', '.delete-product', (e) => {
                const productId = $(e.target).data('id');
                this.deleteProduct(productId);
            });

            $('#editProductBtn').on('click', () => this.enableEditMode());
            $('#saveProduc').on('click', () => this.saveProductChanges());

            // Sự kiện xuất PDF và Excel
            $('#exportProductsPDF').on('click', (e) => {
                e.preventDefault();
                if (this.products.length === 0) {
                    Swal.fire({
                        icon: 'warning',
                        title: 'Không có dữ liệu',
                        text: 'Vui lòng tải danh sách sản phẩm trước khi xuất PDF!',
                    });
                } else {
                    this.exportToPDF();
                }
            });

            $('#exportProductsExcel').on('click', (e) => {
                e.preventDefault();
                if (this.products.length === 0) {
                    Swal.fire({
                        icon: 'warning',
                        title: 'Không có dữ liệu',
                        text: 'Vui lòng tải danh sách sản phẩm trước khi xuất Excel!',
                    });
                } else {
                    this.exportToExcel();
                }
            });
        }


        // Các hàm hiện có giữ nguyên
        previewImage(inputId, previewId) {
            const url = $(inputId).val().trim();
            const preview = $(previewId);
            if (url) {
                preview.attr('src', url).show();
                preview.on('error', () => {
                    preview.hide();
                    Utils.showAlert('warning', 'Cảnh báo', 'Không thể tải hình ảnh từ URL này!');
                });
            } else {
                preview.hide().attr('src', '');
            }
        }

        loadUnitList() {
            const select = $('#unit-name');
            select.empty();
            select.append('<option selected disabled value="">Chọn đơn vị tính</option>');
            this.unitList.forEach(unit => {
                select.append(`<option value="${unit}">${unit}</option>`);
            });
            select.append('<option value="add-unit">Thêm đơn vị tính mới...</option>');
        }

        showCustomUnitInput() {
            $('#new-unit-name').val('').removeClass('is-invalid');
            const modal = new bootstrap.Modal(document.getElementById('addUnitModal'));
            modal.show();
        }

        saveNewUnit() {
            const newUnit = $('#new-unit-name').val().trim();
            if (!newUnit) {
                $('#new-unit-name').addClass('is-invalid');
                return;
            }
            if (this.unitList.includes(newUnit)) {
                $('#new-unit-name').addClass('is-invalid');
                $('#new-unit-name').next('.invalid-feedback').text('Đơn vị tính này đã tồn tại!');
                return;
            }

            this.unitList.push(newUnit);
            this.loadUnitList();
            $('#unit-name').val(newUnit);
            $('#unit-price').focus();

            const modal = bootstrap.Modal.getInstance(document.getElementById('addUnitModal'));
            modal.hide();
        }

        loadDoiTuong(selectId = '#doi_tuong_id', onSuccess = null, onError = null) {
            $.ajax({
                url: '/api/quanly/doituong',
                method: 'GET',
                success: (data) => {
                    const select = $(selectId);
                    select.empty();
                    select.append('<option value="">Chọn đối tượng</option>');
                    data.forEach((doiTuong) => {
                        select.append(`<option value="${doiTuong.id}">${doiTuong.tenDoiTuong}</option>`);
                    });
                    if (selectId === '#doi_tuong_id') {
                        select.append('<option value="add-doi-tuong">Thêm đối tượng sử dụng mới...</option>');
                    }
                    if (typeof onSuccess === 'function') {
                        onSuccess(data);
                    }
                },
                error: (xhr) => {
                    Utils.showAlert('error', 'Lỗi', 'Không thể tải danh sách đối tượng!');
                    if (typeof onError === 'function') {
                        onError(xhr);
                    }
                }
            });
        }



        showAddDoiTuongModal() {
            $('#addDoiTuongForm')[0].reset();
            const modal = new bootstrap.Modal(document.getElementById('addDoiTuongModal'));
            modal.show();
        }

        saveDoiTuong() {
            const tenDoiTuong = $('#doi_tuong_name').val().trim();
            const imageMax = $('#doi_tuong_image_max').val().trim() || null;
            const imageMin = $('#doi_tuong_image_min').val().trim() || null;
            const ghiChu = $('#doi_tuong_note').val().trim() || null;

            if (!tenDoiTuong) {
                Utils.showAlert('warning', 'Cảnh báo', 'Vui lòng nhập tên đối tượng!');
                return;
            }

            const data = { tenDoiTuong, imageMax, imageMin, ghiChu };

            $.ajax({
                url: '/api/quanly/doituong',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(data),
                success: () => {
                    const modal = bootstrap.Modal.getInstance(document.getElementById('addDoiTuongModal'));
                    modal.hide();
                    $('#addDoiTuongForm')[0].reset();
                    this.loadDoiTuong();
                    Utils.showAlert('success', 'Thành công', `Đối tượng "${tenDoiTuong}" đã được thêm!`);
                },
                error: () => Utils.showAlert('error', 'Lỗi', 'Không thể thêm đối tượng!')
            });
        }

        addImage(e) {
            e.preventDefault();
            const imageUrl = $('#image-url').val().trim();
            const isMain = $('#image-is-main').is(':checked');
            const imageOrder = parseInt($('#image-order').val()) || 0;

            if (!imageUrl) {
                Utils.showAlert('warning', 'Cảnh báo', 'Vui lòng nhập URL ảnh!');
                return;
            }
            if (isMain && this.imageList.some(img => img.isMain)) {
                Utils.showAlert('warning', 'Cảnh báo', 'Chỉ được chọn một ảnh làm ảnh chính! Ảnh chính trước đó sẽ bị bỏ chọn.');
                this.imageList.forEach(img => img.isMain = false);
            }

            this.imageList.push({ imageUrl, isMain, imageOrder });
            this.updateImageTable();

            $('#image-url').val('');
            $('#image-is-main').prop('checked', false);
            $('#image-order').val('0');
            $('#image-preview').hide();
        }

        addDetailImage(e) {
            e.preventDefault();
            const imageUrl = $('#detail_image_url').val().trim();
            const isMain = $('#detail_image_is_main').is(':checked');
            const imageOrder = parseInt($('#detail_image_order').val()) || 0;

            if (!imageUrl) {
                Utils.showAlert('warning', 'Cảnh báo', 'Vui lòng nhập URL ảnh!');
                return;
            }
            if (isMain && this.imageList.some(img => img.isMain)) {
                Utils.showAlert('warning', 'Cảnh báo', 'Chỉ được chọn một ảnh làm ảnh chính! Ảnh chính trước đó sẽ bị bỏ chọn.');
                this.imageList.forEach(img => img.isMain = false);
            }

            this.imageList.push({ imageUrl, isMain, imageOrder });
            this.updateDetailImageTable();

            $('#detail_image_url').val('');
            $('#detail_image_is_main').prop('checked', false);
            $('#detail_image_order').val('0');
            $('#detail_image_preview').hide();
        }

        updateImageTable() {
            const tbody = $('#image-table-body');
            tbody.empty();
            this.imageList.forEach((img, idx) => {
                tbody.append(`
                    <tr data-index="${idx}">
                        <td>${img.imageUrl}</td>
                        <td>${img.isMain ? 'Có' : 'Không'}</td>
                        <td>${img.imageOrder}</td>
                        <td><img src="${img.imageUrl}" style="max-width: 50px;" onerror="this.style.display='none';"></td>
                        <td><button class="btn btn-danger btn-sm remove-image" data-index="${idx}">Xóa</button></td>
                    </tr>
                `);
            });

            $('.remove-image').off('click').on('click', (e) => {
                const index = $(e.target).data('index');
                this.imageList.splice(index, 1);
                this.updateImageTable();
            });
        }

        updateDetailImageTable() {
            const tbody = $('#detail_image_table_body');
            tbody.empty();
            this.imageList.forEach((img, idx) => {
                tbody.append(`
                    <tr data-index="${idx}">
                        <td>${img.imageUrl}</td>
                        <td>${img.isMain ? 'Có' : 'Không'}</td>
                        <td>${img.imageOrder}</td>
                        <td><img src="${img.imageUrl}" style="max-width: 100px;" onerror="this.style.display='none';"></td>
                        <td><button class="btn btn-danger btn-sm remove-detail-image" data-index="${idx}" disabled>Xóa</button></td>
                    </tr>
                `);
            });

            $('.remove-detail-image').off('click').on('click', (e) => {
                const index = $(e.target).data('index');
                this.imageList.splice(index, 1);
                this.updateDetailImageTable();
            });
        }

        addUnit(e) {
            e.preventDefault();
            const unitName = $('#unit-name').val();
            const unitPrice = parseFloat($('#unit-price').val()) || 0;
            const unitDiscount = parseFloat($('#unit-discount').val()) || 0;
            const unitNote = $('#unit-note').val().trim();

            if (!unitName || unitPrice <= 0) {
                Utils.showAlert('warning', 'Cảnh báo', 'Vui lòng nhập đầy đủ đơn vị tính và giá!');
                return;
            }
            if (unitName === 'add-unit') {
                Utils.showAlert('warning', 'Cảnh báo', 'Vui lòng chọn hoặc thêm đơn vị tính mới!');
                return;
            }

            this.unitDataList.push({ donViTinh: unitName, gia: unitPrice, discount: unitDiscount, ghiChu: unitNote });
            this.updateUnitTable();

            $('#unit-name').val('');
            $('#unit-price').val('');
            $('#unit-discount').val('0');
            $('#unit-note').val('');
        }

        addDetailUnit(e) {
            e.preventDefault();
            const unitName = $('#detail_unit_name').val();
            const unitPrice = parseFloat($('#detail_unit_price').val()) || 0;
            const unitDiscount = parseFloat($('#detail_unit_discount').val()) || 0;
            const unitNote = $('#detail_unit_note').val().trim();

            if (!unitName || unitPrice <= 0) {
                Utils.showAlert('warning', 'Cảnh báo', 'Vui lòng nhập đầy đủ đơn vị tính và giá!');
                return;
            }

            this.unitDataList.push({ donViTinh: unitName, gia: unitPrice, discount: unitDiscount, ghiChu: unitNote });
            this.updateDetailUnitTable();

            $('#detail_unit_name').val('');
            $('#detail_unit_price').val('');
            $('#detail_unit_discount').val('0');
            $('#detail_unit_note').val('');
        }

        updateUnitTable() {
            const tbody = $('#unit-table-body');
            tbody.empty();
            this.unitDataList.forEach((unit, idx) => {
                tbody.append(`
                    <tr data-index="${idx}">
                        <td>${unit.donViTinh}</td>
                        <td>${unit.gia.toLocaleString('vi-VN')}</td>
                        <td>${unit.discount}%</td>
                        <td>${unit.ghiChu || 'Không'}</td>
                        <td><button class="btn btn-danger btn-sm remove-unit" data-index="${idx}">Xóa</button></td>
                    </tr>
                `);
            });

            $('.remove-unit').off('click').on('click', (e) => {
                const index = $(e.target).data('index');
                this.unitDataList.splice(index, 1);
                this.updateUnitTable();
            });
        }

        updateDetailUnitTable() {
            const tbody = $('#detail_unit_table_body');
            tbody.empty();
            this.unitDataList.forEach((unit, idx) => {
                tbody.append(`
                    <tr data-index="${idx}">
                        <td>${unit.donViTinh}</td>
                        <td>${unit.gia.toLocaleString('vi-VN')}</td>
                        <td>${unit.discount}%</td>
                        <td>${unit.ghiChu || 'Không'}</td>
                        <td><button class="btn btn-danger btn-sm remove-detail-unit" data-index="${idx}" disabled>Xóa</button></td>
                    </tr>
                `);
            });

            $('.remove-detail-unit').off('click').on('click', (e) => {
                const index = $(e.target).data('index');
                this.unitDataList.splice(index, 1);
                this.updateDetailUnitTable();
            });
        }

        saveProduct(e) {
            e.preventDefault();
            const productData = {
                tenSanPham: $('#ten_san_pham').val().trim(),
                thuongHieu: $('#thuong_hieu').val().trim() || null,
                congDung: $('#cong_dung').val().trim() || null,
                doiTuongId: $('#doi_tuong_id').val() ? parseInt($('#doi_tuong_id').val()) : null,
                danhMucId: parseInt($('#danhmuc_id').val()),
                dangBaoChe: $('#dang_bao_che').val().trim() || null,
                quyCach: $('#quy_cach').val().trim() || null,
                xuatXuThuongHieu: $('#xuat_xu_thuong_hieu').val().trim() || null,
                nhaSanXuat: $('#nha_san_xuat').val().trim() || null,
                nuocSanXuat: $('#nuoc_san_xuat').val().trim() || null,
                soDangKy: $('#so_dang_ky').val().trim() || null,
                thanhPhan: $('#thanh_phan').val().trim() || null,
                moTaNgan: $('#mo_ta_ngan').val().trim() || null,
                images: this.imageList,
                donViTinhList: this.unitDataList,
                chiTietSanPham: {
                    moTaChiTiet: $('#mota_chi_tiet').val().trim() || null,
                    thanhPhanChiTiet: $('#thanhphan_chi_tiet').val().trim() || null,
                    congDungChiTiet: $('#cong_dung_chi_tiet').val().trim() || null,
                    cachDungChiTiet: $('#cach_dung_chi_tiet').val().trim() || null,
                    tacDungPhu: $('#tac_dung_phu').val().trim() || null,
                    luuY: $('#luu_y').val().trim() || null,
                    baoQuan: $('#ban_quan').val().trim() || null
                }
            };

            if (!productData.tenSanPham || !productData.danhMucId) {
                Utils.showAlert('warning', 'Cảnh báo', 'Vui lòng nhập đầy đủ các trường bắt buộc!');
                return;
            }
            if (this.imageList.length === 0) {
                Utils.showAlert('warning', 'Cảnh báo', 'Phải thêm ít nhất một ảnh!');
                return;
            }
            if (this.unitDataList.length === 0) {
                Utils.showAlert('warning', 'Cảnh báo', 'Phải thêm ít nhất một đơn vị tính!');
                return;
            }

            $.ajax({
                url: '/api/quanly/products',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(productData),
                success: (newProduct) => {
                    Utils.showAlert('success', 'Thành công', 'Sản phẩm đã được thêm!');
                    $('#addProductModal').modal('hide');
                    this.addProductToTable(newProduct);
                    this.loadProducts(this.currentPage); // Cập nhật danh sách sản phẩm
                },
                error: (xhr) => Utils.showAlert('error', 'Lỗi', xhr.responseText || 'Không thể lưu sản phẩm!')
            });
        }

        addProductToTable(product) {
            this.products.unshift(product); // Thêm sản phẩm mới vào đầu danh sách
            this.renderProducts(this.products, this.currentPage, this.pageSize);
        }

        loadProducts(page = 1) {
            this.currentPage = page;
            const search = $('#search-products').val().trim();
            const category = $('#filter-category').val();
            const pageSize = this.pageSize;

            $.ajax({
                url: '/api/quanly/products',
                method: 'GET',
                success: (products) => {
                    this.products = products.filter(p =>
                        p.tenSanPham.toLowerCase().includes(search.toLowerCase()) &&
                        (!category || p.danhMucId == category)
                    );
                    this.renderProducts(this.products, page, pageSize);
                },
                error: () => Utils.showAlert('error', 'Lỗi', 'Không thể tải sản phẩm!')
            });
        }

        renderProducts(products, page, pageSize) {
            const tbody = $('#products-tbody');
            tbody.empty();
            const start = (page - 1) * pageSize;
            const end = start + pageSize;
            const paginated = products.slice(start, end);

            paginated.forEach(p => {
                tbody.append(`
                    <tr data-id="${p.id}">
                        <td>${p.id}</td>
                        <td>${p.tenSanPham}</td>
                        <td>${p.thuongHieu || 'N/A'}</td>
                        <td>${p.lowestPrice ? `${Number(p.lowestPrice).toLocaleString('vi-VN')} VNĐ/${p.donViTinh || 'N/A'}` : 'N/A'}</td>
                        <td>${p.soLuong !== undefined && p.soLuong !== null ? p.soLuong : '0'}</td>
                        <td><img src="${p.mainImageUrl || ''}" style="width: 50px; height: 50px;" onerror="this.style.display='none';"></td>
                        <td>${p.manufactureDate || 'N/A'} / ${p.expiryDate || 'N/A'}</td>
                        <td>
                            <button class="btn btn-info btn-sm view-product" data-id="${p.id}" data-bs-toggle="modal" data-bs-target="#productDetailModal">Chi tiết</button>
                            <button class="btn btn-danger btn-sm delete-product" data-id="${p.id}">Xóa</button>
                        </td>
                    </tr>
                `);
            });
            this.updatePagination(products.length, page, pageSize, '#products-pagination', this.loadProducts.bind(this));
        }

        updatePagination(totalItems, currentPage, pageSize, paginationId, callback) {
            const totalPages = Math.ceil(totalItems / pageSize);
            const pagination = $(paginationId);
            pagination.empty();
            for (let i = 1; i <= totalPages; i++) {
                pagination.append(`
                    <li class="page-item ${i === currentPage ? 'active' : ''}">
                        <a class="page-link" href="#" data-page="${i}">${i}</a>
                    </li>
                `);
            }
            pagination.find('.page-link').on('click', (e) => {
                e.preventDefault();
                callback($(e.target).data('page'));
            });
        }

        showProductDetail(productId) {
            $.ajax({
                url: `/api/quanly/products/${productId}`,
                method: 'GET',
                success: (data) => {
                    this.populateProductDetail(data);
                    $('#productDetailModal').data('productId', productId);
                    const modal = new bootstrap.Modal(document.getElementById('productDetailModal'));
                    modal.show();
                    $('#editProductBtn').show();
                    $('#saveProduc').hide();
                    $('#productDetailForm input, #productDetailForm textarea, #productDetailForm select').prop('readonly', true).prop('disabled', true);
                    $('.remove-detail-image, .remove-detail-unit').prop('disabled', true);
                },
                error: (xhr) => Utils.showAlert('error', 'Lỗi', xhr.responseText || 'Không thể tải chi tiết sản phẩm!')
            });
        }

        populateProductDetail(data) {
            $('#productDetailModalLabel').text(`Chi tiết sản phẩm: ${data.tenSanPham}`);
            $('#detail_ten_san_pham').val(data.tenSanPham || '');
            $('#detail_thuong_hieu').val(data.thuongHieu || '');
            $('#detail_cong_dung').val(data.congDung || '');
            $('#detail_dang_bao_che').val(data.dangBaoChe || '');
            $('#detail_quy_cach').val(data.quyCach || '');
            $('#detail_xuat_xu_thuong_hieu').val(data.xuatXuThuongHieu || '');
            $('#detail_nha_san_xuat').val(data.nhaSanXuat || '');
            $('#detail_nuoc_san_xuat').val(data.nuocSanXuat || '');
            $('#detail_so_dang_ky').val(data.soDangKy || '');
            $('#detail_thanh_phan').val(data.thanhPhan || '');
            $('#detail_mo_ta_ngan').val(data.moTaNgan || '');

            // Tải danh sách đối tượng sử dụng và danh mục trước khi gán giá trị
            Promise.all([
                new Promise((resolve, reject) => {
                    this.loadDoiTuong('#detail_doi_tuong', () => {
                        $('#detail_doi_tuong').val(data.doiTuongId || '');
                        resolve();
                    }, reject);
                }),
                new Promise((resolve, reject) => {
                    Utils.loadCategories('#detail_danh_muc', 'Chọn danh mục', {
                        onSuccess: () => {
                            $('#detail_danh_muc').val(data.danhMucId || '');
                            resolve();
                        },
                        onError: reject
                    });
                })
            ]).then(() => {
                // Tiếp tục các thao tác khác sau khi đã gán giá trị
                this.imageList = data.images || [];
                this.updateDetailImageTable();

                this.unitDataList = data.donViTinhList || [];
                this.updateDetailUnitTable();

                const unitSelect = $('#detail_unit_name');
                unitSelect.empty();
                unitSelect.append('<option value="">Chọn đơn vị tính</option>');
                this.unitList.forEach(unit => {
                    unitSelect.append(`<option value="${unit}">${unit}</option>`);
                });

                const chiTiet = data.chiTietSanPham || {};
                $('#detail_mota_chi_tiet').val(chiTiet.moTaChiTiet || '');
                $('#detail_thanhphan_chi_tiet').val(chiTiet.thanhPhanChiTiet || '');
                $('#detail_cong_dung_chi_tiet').val(chiTiet.congDungChiTiet || '');
                $('#detail_cach_dung_chi_tiet').val(chiTiet.cachDungChiTiet || '');
                $('#detail_tac_dung_phu').val(chiTiet.tacDungPhu || '');
                $('#detail_luu_y').val(chiTiet.luuY || '');
                $('#detail_ban_quan').val(chiTiet.baoQuan || '');

                const inventoryTbody = $('#detail_inventory_table_body');
                inventoryTbody.empty();
                $.ajax({
                    url: `/api/quanly/inventory-by-product/${data.id}`,
                    method: 'GET',
                    success: (inventoryList) => {
                        if (inventoryList.length === 0) {
                            inventoryTbody.append('<tr><td colspan="6">Không có dữ liệu kho</td></tr>');
                        } else {
                            inventoryList.forEach(item => {
                                const today = new Date();
                                const expiryDate = new Date(item.expiryDate);
                                let statusClass = '';
                                let statusText = '';
                                const daysLeft = Math.ceil((expiryDate - today) / (1000 * 60 * 60 * 24));

                                if (daysLeft < 0) {
                                    statusClass = 'inventory-status-expired';
                                    statusText = 'Hết hạn';
                                } else if (daysLeft <= 30) {
                                    statusClass = 'inventory-status-near-expiry';
                                    statusText = 'Sắp hết hạn';
                                } else {
                                    statusClass = 'inventory-status-valid';
                                    statusText = 'Còn hạn';
                                }

                                inventoryTbody.append(`
                    <tr>
                        <td>${item.kho?.tenKho || 'N/A'}</td>
                        <td>${item.batchNumber}</td>
                        <td>${item.quantity}</td>
                        <td>${item.manufactureDate || 'N/A'}</td>
                        <td>${item.expiryDate || 'N/A'}</td>
                        <td><span class="${statusClass}">${statusText}</span></td>
                    </tr>
                `);
                            });
                        }
                    },
                    error: () => inventoryTbody.append('<tr><td colspan="6">Không có dữ liệu kho</td></tr>')
                });
            }).catch((error) => {
                Utils.showAlert('error', 'Lỗi', 'Không thể tải danh sách đối tượng hoặc danh mục!');
                console.error('Error loading dropdowns:', error);
            });
        }



        enableEditMode() {
            const productId = $('#productDetailModal').data('productId');

            // Kích hoạt các trường input/select
            $('#productDetailForm input, #productDetailForm textarea, #productDetailForm select').prop('readonly', false).prop('disabled', false);
            $('.remove-detail-image, .remove-detail-unit').prop('disabled', false);
            $('#editProductBtn').hide();
            $('#saveProduc').show();

            // Tải danh sách đối tượng và danh mục, sau đó gán giá trị
            $.ajax({
                url: `/api/quanly/products/${productId}`,
                method: 'GET',
                success: (data) => {
                    // Tải danh sách đối tượng
                    this.loadDoiTuong('#detail_doi_tuong', () => {
                        $('#detail_doi_tuong').val(data.doiTuongId || '');
                    });

                    // Tải danh sách danh mục
                    Utils.loadCategories('#detail_danh_muc', 'Chọn danh mục', {
                        onSuccess: () => {
                            $('#detail_danh_muc').val(data.danhMucId || '');
                        }
                    });
                },
                error: () => {
                    Utils.showAlert('error', 'Lỗi', 'Không thể tải dữ liệu sản phẩm!');
                }
            });
        }

        saveProductChanges() {
            const productId = $('#productDetailModal').data('productId');
            const updatedData = {
                tenSanPham: $('#detail_ten_san_pham').val().trim(),
                thuongHieu: $('#detail_thuong_hieu').val().trim() || null,
                congDung: $('#detail_cong_dung').val().trim() || null,
                doiTuongId: $('#detail_doi_tuong').val() ? parseInt($('#detail_doi_tuong').val()) : null,
                danhMucId: parseInt($('#detail_danh_muc').val()),
                dangBaoChe: $('#detail_dang_bao_che').val().trim() || null,
                quyCach: $('#detail_quy_cach').val().trim() || null,
                xuatXuThuongHieu: $('#detail_xuat_xu_thuong_hieu').val().trim() || null,
                nhaSanXuat: $('#detail_nha_san_xuat').val().trim() || null,
                nuocSanXuat: $('#detail_nuoc_san_xuat').val().trim() || null,
                soDangKy: $('#detail_so_dang_ky').val().trim() || null,
                thanhPhan: $('#detail_thanh_phan').val().trim() || null,
                moTaNgan: $('#detail_mo_ta_ngan').val().trim() || null,
                images: this.imageList,
                donViTinhList: this.unitDataList,
                chiTietSanPham: {
                    moTaChiTiet: $('#detail_mota_chi_tiet').val().trim() || null,
                    thanhPhanChiTiet: $('#detail_thanhphan_chi_tiet').val().trim() || null,
                    congDungChiTiet: $('#detail_cong_dung_chi_tiet').val().trim() || null,
                    cachDungChiTiet: $('#detail_cach_dung_chi_tiet').val().trim() || null,
                    tacDungPhu: $('#detail_tac_dung_phu').val().trim() || null,
                    luuY: $('#detail_luu_y').val().trim() || null,
                    baoQuan: $('#detail_ban_quan').val().trim() || null
                }
            };

            if (!updatedData.tenSanPham || !updatedData.danhMucId) {
                Utils.showAlert('warning', 'Cảnh báo', 'Vui lòng nhập đầy đủ các trường bắt buộc!');
                return;
            }
            if (this.imageList.length === 0) {
                Utils.showAlert('warning', 'Cảnh báo', 'Phải có ít nhất một ảnh!');
                return;
            }
            if (this.unitDataList.length === 0) {
                Utils.showAlert('warning', 'Cảnh báo', 'Phải có ít nhất một đơn vị tính!');
                return;
            }

            $.ajax({
                url: `/api/quanly/products/${productId}`,
                method: 'PUT',
                contentType: 'application/json',
                data: JSON.stringify(updatedData),
                success: () => {
                    Utils.showAlert('success', 'Thành công', 'Sản phẩm đã được cập nhật!');
                    $('#productDetailModal').modal('hide');
                    this.loadProducts(this.currentPage);
                },
                error: (xhr) => Utils.showAlert('error', 'Lỗi', xhr.responseText || 'Không thể cập nhật sản phẩm!')
            });
        }

        deleteProduct(productId) {
            Swal.fire({
                title: 'Xác nhận xóa',
                text: 'Bạn có chắc chắn muốn xóa sản phẩm này?',
                icon: 'warning',
                showCancelButton: true,
                confirmButtonText: 'Xóa',
                cancelButtonText: 'Hủy'
            }).then((result) => {
                if (result.isConfirmed) {
                    $.ajax({
                        url: `/api/quanly/products/${productId}`,
                        method: 'DELETE',
                        success: () => {
                            Utils.showAlert('success', 'Thành công', 'Sản phẩm đã được xóa!');
                            this.products = this.products.filter(p => p.id !== productId);
                            this.renderProducts(this.products, this.currentPage, this.pageSize);
                        },
                        error: () => Utils.showAlert('error', 'Lỗi', 'Không thể xóa sản phẩm!')
                    });
                }
            });
        }

        // Hàm xuất PDF
        exportToPDF() {
            const doc = new jsPDF();

            // Thêm font DejaVu Sans
            doc.addFileToVFS("DejaVuSans.ttf", dejaVuSansBase64);
            doc.addFont("DejaVuSans.ttf", "DejaVuSans", "normal");
            doc.setFont("DejaVuSans"); // Sử dụng font DejaVuSans

            const pageWidth = doc.internal.pageSize.getWidth();
            const margin = 10;

            // Tiêu đề
            doc.setFontSize(20);
            doc.setTextColor(0, 102, 204); // Màu xanh dương
            doc.text("Danh Sách Sản Phẩm", pageWidth / 2, 20, { align: "center" });

            // Thông tin ngày xuất và tổng số sản phẩm
            doc.setFontSize(12);
            doc.setTextColor(0, 0, 0); // Màu đen
            doc.text(`Ngày xuất: ${new Date().toLocaleDateString('vi-VN')}`, margin, 30);
            doc.text(`Tổng số sản phẩm: ${this.products.length}`, margin, 38);

            // Định nghĩa dữ liệu bảng
            const tableData = this.products.map(product => [
                product.id.toString(),
                product.tenSanPham,
                product.thuongHieu || 'N/A',
                product.lowestPrice ? `${Number(product.lowestPrice).toLocaleString('vi-VN')} VNĐ/${product.donViTinh || 'N/A'}` : 'N/A',
                (product.soLuong !== undefined && product.soLuong !== null ? product.soLuong : '0').toString(),
                `${product.manufactureDate || 'N/A'} / ${product.expiryDate || 'N/A'}`
            ]);

            // Vẽ bảng bằng autotable
            doc.autoTable({
                startY: 45,
                head: [['ID', 'Tên sản phẩm', 'Thương hiệu', 'Giá (VNĐ)', 'Số lượng', 'Ngày SX/HSD']],
                body: tableData,
                theme: 'grid',
                headStyles: {
                    fillColor: [0, 102, 204], // Màu xanh dương
                    textColor: [255, 255, 255], // Chữ trắng
                    fontSize: 12,
                    font: 'DejaVuSans', // Font tiếng Việt
                    halign: 'center'
                },
                bodyStyles: {
                    fontSize: 10,
                    font: 'DejaVuSans', // Font tiếng Việt
                    textColor: [0, 0, 0],
                    lineWidth: 0.1,
                    lineColor: [0, 0, 0]
                },
                columnStyles: {
                    0: { cellWidth: 15, halign: 'center' },
                    1: { cellWidth: 50 },
                    2: { cellWidth: 30 },
                    3: { cellWidth: 30, halign: 'right' },
                    4: { cellWidth: 20, halign: 'center' },
                    5: { cellWidth: 40, halign: 'center' }
                },
                margin: { top: 45, left: margin, right: margin },
                didDrawPage: (data) => {
                    // Thêm số trang
                    const pageCount = doc.internal.getNumberOfPages();
                    const pageNumber = doc.internal.getCurrentPageInfo().pageNumber;
                    doc.setFontSize(10);
                    doc.setFont("DejaVuSans");
                    doc.text(`Trang ${pageNumber}/${pageCount}`, pageWidth - margin, doc.internal.pageSize.getHeight() - 10, { align: 'right' });
                }
            });

            // Lưu file PDF
            doc.save("Danh_sach_san_pham.pdf");
        }

        exportToExcel() {
            // Kiểm tra xem ExcelJS có được tải không
            if (typeof ExcelJS === 'undefined') {
                Swal.fire({
                    icon: 'error',
                    title: 'Lỗi',
                    text: 'Thư viện ExcelJS không được tải. Vui lòng kiểm tra lại!',
                });
                return;
            }

            const workbook = new ExcelJS.Workbook();
            const worksheet = workbook.addWorksheet('Sản phẩm');

            // Thêm tiêu đề chính
            worksheet.mergeCells('A1:F1');
            worksheet.getCell('A1').value = 'Danh Sách Sản Phẩm';
            worksheet.getCell('A1').alignment = { horizontal: 'center', vertical: 'middle' };
            worksheet.getCell('A1').font = { size: 16, bold: true };
            worksheet.getCell('A1').fill = {
                type: 'pattern',
                pattern: 'solid',
                fgColor: { argb: 'FF0066CC' } // Màu xanh dương
            };
            worksheet.getCell('A1').border = {
                top: { style: 'thin' },
                left: { style: 'thin' },
                bottom: { style: 'thin' },
                right: { style: 'thin' }
            };

            // Thêm thông tin ngày xuất và tổng số
            worksheet.getCell('A2').value = `Ngày xuất: ${new Date().toLocaleDateString('vi-VN')}`;
            worksheet.getCell('A3').value = `Tổng số sản phẩm: ${this.products.length}`;

            // Thêm tiêu đề cột
            const headers = ['ID', 'Tên sản phẩm', 'Thương hiệu', 'Giá (VNĐ)', 'Số lượng', 'Ngày SX/HSD'];
            worksheet.addRow(headers);
            worksheet.getRow(5).eachCell(cell => {
                cell.font = { bold: true };
                cell.alignment = { horizontal: 'center', vertical: 'middle' };
                cell.fill = {
                    type: 'pattern',
                    pattern: 'solid',
                    fgColor: { argb: 'FF0066CC' }
                };
                cell.border = {
                    top: { style: 'thin' },
                    left: { style: 'thin' },
                    bottom: { style: 'thin' },
                    right: { style: 'thin' }
                };
            });

            // Thêm dữ liệu
            this.products.forEach(product => {
                worksheet.addRow([
                    product.id,
                    product.tenSanPham,
                    product.thuongHieu || 'N/A',
                    product.lowestPrice ? `${Number(product.lowestPrice).toLocaleString('vi-VN')} VNĐ/${product.donViTinh || 'N/A'}` : 'N/A',
                    product.soLuong !== undefined && product.soLuong !== null ? product.soLuong : 0,
                    `${product.manufactureDate || 'N/A'} / ${product.expiryDate || 'N/A'}`
                ]).eachCell(cell => {
                    cell.border = {
                        top: { style: 'thin' },
                        left: { style: 'thin' },
                        bottom: { style: 'thin' },
                        right: { style: 'thin' }
                    };
                });
            });

            // Tùy chỉnh chiều rộng cột
            worksheet.columns = [
                { width: 10 },  // ID
                { width: 40 },  // Tên sản phẩm
                { width: 20 },  // Thương hiệu
                { width: 25 },  // Giá (VNĐ)
                { width: 15 },  // Số lượng
                { width: 25 }   // Ngày SX/HSD
            ];

            // Xuất file
            workbook.xlsx.writeBuffer().then(buffer => {
                const blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = 'Danh_sach_san_pham.xlsx';
                a.click();
                window.URL.revokeObjectURL(url);
            }).catch(error => {
                console.error('Lỗi khi xuất Excel:', error);
                Swal.fire({
                    icon: 'error',
                    title: 'Lỗi',
                    text: 'Không thể xuất file Excel. Vui lòng thử lại!',
                });
            });
        }

    }
    new ProductManager();


    $('#productDetailModal').on('hidden.bs.modal', function () {
        $('.modal-backdrop').remove();
        $('body').removeClass('modal-open');
    });
    $('#productDetailModal').on('shown.bs.modal', function () {
        const productId = $(this).data('productId');
        if (productId) {
            $.ajax({
                url: `/api/quanly/products/${productId}`,
                method: 'GET',
                success: (data) => {
                    // Đảm bảo các trường select được làm mới
                    $('#detail_doi_tuong').val(data.doiTuongId || '');
                    $('#detail_danh_muc').val(data.danhMucId || '');
                },
                error: () => {
                    Utils.showAlert('error', 'Lỗi', 'Không thể làm mới dữ liệu sản phẩm!');
                }
            });
        }
    });
});