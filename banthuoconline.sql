CREATE TABLE danhmuc (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ghi_chu VARCHAR(255) NULL,
    ten_danh_muc VARCHAR(255) NOT NULL
);

CREATE TABLE doituong (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ghi_chu VARCHAR(255) NULL,
    image_max VARCHAR(255) NOT NULL,
    image_min VARCHAR(255) NOT NULL,
    ten_doi_tuong VARCHAR(255) NOT NULL
);

CREATE TABLE product (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cong_dung VARCHAR(255) NULL,
    dang_bao_che VARCHAR(255) NULL,
    mo_ta_ngan VARCHAR(255) NULL,
    nha_san_xuat VARCHAR(255) NULL,
    nuoc_san_xuat VARCHAR(255) NULL,
    quy_cach VARCHAR(255) NULL,
    so_dang_ky VARCHAR(255) NULL,
    so_luong INT NULL,
    ten_san_pham VARCHAR(255) NOT NULL,
    thanh_phan VARCHAR(255) NULL,
    thuong_hieu VARCHAR(255) NULL,
    xuat_xu_thuong_hieu VARCHAR(255) NULL,
    danhmuc_id INT NULL,
    doi_tuong_id INT NULL,
    FOREIGN KEY (danhmuc_id) REFERENCES danhmuc(id),
    FOREIGN KEY (doi_tuong_id) REFERENCES doituong(id)
);

CREATE TABLE cauhoilienquan (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cau_hoi TEXT NOT NULL,
    cau_tra_loi TEXT NOT NULL,
    product_id INT NOT NULL,
    FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE chitietsanpham (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ban_quan TEXT NULL,
    cach_dung_chi_tiet TEXT NULL,
    cong_dung_chi_tiet TEXT NULL,
    luu_y TEXT NULL,
    mota_chi_tiet TEXT NULL,
    product_id INT NOT NULL,
    tac_dung_phu TEXT NULL,
    thanhphan_chi_tiet TEXT NULL,
    FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE don_vi_tinh (
    id INT AUTO_INCREMENT PRIMARY KEY,
    discount DECIMAL(38,2) NULL,
    don_vi_tinh VARCHAR(255) NULL,
    ghi_chu VARCHAR(255) NULL,
    gia DECIMAL(38,2) NOT NULL,
    product_id INT NOT NULL,
    FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE product_images (
    id INT AUTO_INCREMENT PRIMARY KEY,
    image_order INT NULL,
    image_url VARCHAR(255) NOT NULL,
    is_main BIT NULL,
    product_id INT NOT NULL,
    FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE user (
    id BINARY(16) NOT NULL PRIMARY KEY,
    address VARCHAR(255) NULL,
    date_of_birth DATE NULL,
    email VARCHAR(255) NULL,
    facebook_id VARCHAR(255) NULL,
    google_id VARCHAR(255) NULL,
    name VARCHAR(255) NULL,
    password VARCHAR(255) NULL,
    phone_number VARCHAR(255) NULL,
    picture VARCHAR(255) NULL,
    role VARCHAR(255) NULL
);

CREATE TABLE cart (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id BINARY(16) NULL,
    session_id VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

CREATE INDEX user_id ON cart (user_id);

CREATE TABLE cart_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cart_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT DEFAULT 1 NOT NULL,
    price DECIMAL(38,2) NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NULL,
    don_vi_tinh_id INT NULL,
    total_price DECIMAL(38,2) NULL,
    FOREIGN KEY (don_vi_tinh_id) REFERENCES don_vi_tinh(id),
    FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

CREATE INDEX cart_id ON cart_items (cart_id);
CREATE INDEX product_id ON cart_items (product_id);

CREATE TABLE danhgia (
    id INT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    product_id INT NOT NULL,
    rating INT NOT NULL,
    review_text TEXT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id BINARY(16) NOT NULL,
    FOREIGN KEY (product_id) REFERENCES product(id),
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE TABLE kho (
    id INT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NULL,
    dia_chi VARCHAR(255) NULL,
    dien_tich DECIMAL(10,2) NULL,
    email_lien_he VARCHAR(100) NULL,
    ghi_chu TEXT NULL,
    loai_kho VARCHAR(255) NOT NULL,
    ma_kho VARCHAR(20) NOT NULL,
    ngay_khoi_tao DATE NULL,
    nguoi_quanly VARCHAR(50) NULL,
    so_dien_thoai_lien_he VARCHAR(20) NULL,
    suc_chua INT NULL,
    ten_kho VARCHAR(100) NOT NULL,
    trang_thai ENUM('ACTIVE','INACTIVE','MAINTENANCE') NOT NULL,
    updated_at DATETIME(6) NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    UNIQUE (ma_kho),
    FOREIGN KEY (updated_by) REFERENCES user(id),
    FOREIGN KEY (created_by) REFERENCES user(id)
);

CREATE TABLE nhacungcap (
    id INT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NULL,
    dia_chi VARCHAR(255) NULL,
    email VARCHAR(100) NULL,
    ghi_chu TEXT NULL,
    loai_nha_cung_cap ENUM('NỘI ĐỊA','QUỐC TẾ') NULL,
    ma_nha_cung_cap VARCHAR(20) NULL,
    ngay_hop_tac DATE NULL,
    nguoi_dai_dien VARCHAR(100) NULL,
    so_dien_thoai VARCHAR(20) NULL,
    tax_code VARCHAR(20) NULL,
    ten_nha_cung_cap VARCHAR(100) NOT NULL,
    trang_thai ENUM('HOẠT ĐỘNG','KHÔNG HOẠT ĐỘNG') NULL,
    updated_at DATETIME(6) NULL,
    website VARCHAR(100) NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    UNIQUE (ma_nha_cung_cap),
    UNIQUE (tax_code),
    FOREIGN KEY (created_by) REFERENCES user(id),
    FOREIGN KEY (updated_by) REFERENCES user(id)
);

CREATE TABLE inventory (
    id INT AUTO_INCREMENT PRIMARY KEY,
    batch_number VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NULL,
    expiry_date DATETIME(6) NULL,
    ghichu VARCHAR(255) NULL,
    manufacture_date DATETIME(6) NULL,
    quantity INT NOT NULL,
    storage_location VARCHAR(255) NULL,
    updated_at DATETIME(6) NULL,
    kho_id INT NOT NULL,
    product_id INT NOT NULL,
    supplier_id INT NULL,
    FOREIGN KEY (supplier_id) REFERENCES nhacungcap(id),
    FOREIGN KEY (kho_id) REFERENCES kho(id),
    FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NULL,
    total_price DECIMAL(10,2) NOT NULL,
    status ENUM('pending','completed','canceled') DEFAULT 'pending' NULL,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

CREATE TABLE order_details (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

CREATE INDEX order_id ON order_details (order_id);
CREATE INDEX product_id ON order_details (product_id);

CREATE TABLE order_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    price DECIMAL(38,2) NOT NULL,
    quantity INT NOT NULL,
    don_vi_tinh_id INT NULL,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    FOREIGN KEY (don_vi_tinh_id) REFERENCES don_vi_tinh(id),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE INDEX user_id ON orders (user_id);