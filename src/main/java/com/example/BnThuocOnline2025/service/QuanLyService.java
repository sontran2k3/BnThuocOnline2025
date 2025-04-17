package com.example.BnThuocOnline2025.service;

import com.example.BnThuocOnline2025.dto.InventoryResponseDTO;
import com.example.BnThuocOnline2025.dto.InventoryTransactionDTO;
import com.example.BnThuocOnline2025.dto.ProductDTO;
import com.example.BnThuocOnline2025.model.*;
import com.example.BnThuocOnline2025.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuanLyService {

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private DoiTuongRepository doiTuongRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private DonViTinhRepository donViTinhRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private KhoRepository khoRepository;

    @Autowired
    private NhaCungCapRepository nhaCungCapRepository;

    @Autowired
    private CauHoiLienQuanRepository cauHoiLienQuanRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private DanhGiaRepository danhGiaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Danh mục
    public List<DanhMuc> getAllDanhMuc() {
        return danhMucRepository.findAll();
    }

    public DanhMuc addDanhMuc(DanhMuc danhMuc) {
        return danhMucRepository.save(danhMuc);
    }

    // Đối tượng sử dụng
    public List<DoiTuong> getAllDoiTuong() {
        return doiTuongRepository.findAll();
    }

    public DoiTuong addDoiTuong(DoiTuong doiTuong) {
        return doiTuongRepository.save(doiTuong);
    }

    // Sản phẩm
    public List<ProductDTO> getAllProducts() {
        List<Product> products = productRepository.findAllWithDetails();
        List<ProductDTO> productDTOs = new ArrayList<>();

        for (Product product : products) {
            ProductDTO dto = mapToProductDTO(product);
            productDTOs.add(dto);
        }
        return productDTOs;
    }

    @Transactional
    public ProductDTO getProductDetail(Integer productId) {
        Optional<Product> productOptional = productRepository.findById(productId);
        if (!productOptional.isPresent()) {
            throw new RuntimeException("Sản phẩm không tồn tại!");
        }

        Product product = productOptional.get();
        ProductDTO dto = mapToProductDTO(product);

        List<ProductImage> images = productImageRepository.findByProductId(productId);
        List<ProductDTO.ImageDTO> imageDTOs = new ArrayList<>();
        for (ProductImage image : images) {
            ProductDTO.ImageDTO imageDTO = new ProductDTO.ImageDTO();
            imageDTO.setImageUrl(image.getImageUrl());
            imageDTO.setMain(image.isMain());
            imageDTO.setImageOrder(image.getImageOrder());
            imageDTOs.add(imageDTO);
        }
        dto.setImages(imageDTOs);

        List<DonViTinh> donViTinhList = donViTinhRepository.findByProductId(productId);
        List<ProductDTO.DonViTinhDTO> donViTinhDTOs = new ArrayList<>();
        for (DonViTinh dvt : donViTinhList) {
            ProductDTO.DonViTinhDTO dvtDTO = new ProductDTO.DonViTinhDTO();
            dvtDTO.setDonViTinh(dvt.getDonViTinh());
            dvtDTO.setGia(dvt.getGia().doubleValue());
            dvtDTO.setDiscount(dvt.getDiscount() != null ? dvt.getDiscount().doubleValue() : null);
            dvtDTO.setGhiChu(dvt.getGhiChu());
            donViTinhDTOs.add(dvtDTO);
        }
        dto.setDonViTinhList(donViTinhDTOs);

        Optional<ChiTietSanPham> chiTietOptional = chiTietSanPhamRepository.findByProductId(productId);
        if (chiTietOptional.isPresent()) {
            ChiTietSanPham chiTiet = chiTietOptional.get();
            ProductDTO.ChiTietSanPhamDTO chiTietDTO = new ProductDTO.ChiTietSanPhamDTO();
            chiTietDTO.setMoTaChiTiet(chiTiet.getMoTaChiTiet());
            chiTietDTO.setThanhPhanChiTiet(chiTiet.getThanhPhanChiTiet());
            chiTietDTO.setCongDungChiTiet(chiTiet.getCongDungChiTiet());
            chiTietDTO.setCachDungChiTiet(chiTiet.getCachDungChiTiet());
            chiTietDTO.setTacDungPhu(chiTiet.getTacDungPhu());
            chiTietDTO.setLuuY(chiTiet.getLuuY());
            chiTietDTO.setBaoQuan(chiTiet.getBaoQuan());
            dto.setChiTietSanPham(chiTietDTO);
        }

        List<Inventory> inventories = inventoryRepository.findByProductId(productId);
        dto.setSoLuong(inventories.stream().mapToInt(Inventory::getQuantity).sum());

        return dto;
    }

    private ProductDTO mapToProductDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setTenSanPham(product.getTenSanPham());
        dto.setThuongHieu(product.getThuongHieu());
        dto.setCongDung(product.getCongDung());
        dto.setDoiTuongId(product.getDoiTuong() != null ? product.getDoiTuong().getId() : 0);
        dto.setDanhMucId(product.getDanhMuc() != null ? product.getDanhMuc().getId() : 0);
        dto.setDangBaoChe(product.getDangBaoChe());
        dto.setQuyCach(product.getQuyCach());
        dto.setXuatXuThuongHieu(product.getXuatXuThuongHieu());
        dto.setNhaSanXuat(product.getNhaSanXuat());
        dto.setNuocSanXuat(product.getNuocSanXuat());
        dto.setThanhPhan(product.getThanhPhan());
        dto.setMoTaNgan(product.getMoTaNgan());
        dto.setSoDangKy(product.getSoDangKy());

        // Lấy hình ảnh chính
        Optional<ProductImage> mainImage = productImageRepository.findMainImageByProductId(product.getId());
        dto.setMainImageUrl(mainImage.map(ProductImage::getImageUrl).orElse(""));

        // Lấy giá thấp nhất và đơn vị tính
        List<DonViTinh> donViTinhList = donViTinhRepository.findByProductIdOrderByGiaAsc(product.getId());
        if (!donViTinhList.isEmpty()) {
            DonViTinh lowestPriceUnit = donViTinhList.get(0);
            dto.setLowestPrice(lowestPriceUnit.getGia());
            dto.setDonViTinh(lowestPriceUnit.getDonViTinh());
        } else {
            dto.setLowestPrice(BigDecimal.ZERO);
            dto.setDonViTinh("N/A");
        }

        // Tính tổng số lượng tồn kho từ bảng inventory
        List<Inventory> inventories = inventoryRepository.findByProductId(product.getId());
        int totalQuantity = inventories.stream().mapToInt(Inventory::getQuantity).sum();
        dto.setSoLuong(totalQuantity);

        // Lấy ngày sản xuất và hạn sử dụng từ inventory đầu tiên (nếu có)
        if (!inventories.isEmpty()) {
            Inventory firstInventory = inventories.get(0);
            dto.setManufactureDate(firstInventory.getManufactureDate() != null
                    ? firstInventory.getManufactureDate().format(DATE_FORMATTER)
                    : null);
            dto.setExpiryDate(firstInventory.getExpiryDate() != null
                    ? firstInventory.getExpiryDate().format(DATE_FORMATTER)
                    : null);
        }

        return dto;
    }



    @Transactional
    public void deleteProduct(Integer productId) {
        Optional<Product> productOptional = productRepository.findById(productId);
        if (!productOptional.isPresent()) {
            throw new RuntimeException("Sản phẩm không tồn tại!");
        }

        Product product = productOptional.get();

        List<CauHoiLienQuan> cauHoiLienQuans = cauHoiLienQuanRepository.findByProductId(productId);
        if (!cauHoiLienQuans.isEmpty()) {
            cauHoiLienQuanRepository.deleteAll(cauHoiLienQuans);
        }

        List<ProductImage> productImages = productImageRepository.findByProductId(productId);
        if (!productImages.isEmpty()) {
            productImageRepository.deleteAll(productImages);
        }

        List<Inventory> inventories = inventoryRepository.findByProductId(productId);
        if (!inventories.isEmpty()) {
            inventoryRepository.deleteAll(inventories);
        }

        List<DonViTinh> donViTinhList = donViTinhRepository.findByProductId(productId);
        if (!donViTinhList.isEmpty()) {
            donViTinhRepository.deleteAll(donViTinhList);
        }

        Optional<ChiTietSanPham> chiTietOptional = chiTietSanPhamRepository.findByProductId(productId);
        chiTietOptional.ifPresent(chiTietSanPhamRepository::delete);

        productRepository.delete(product);
    }

    @Transactional
    public Product addProduct(ProductDTO productDTO) {
        Product product = new Product();
        product.setTenSanPham(productDTO.getTenSanPham());
        product.setThuongHieu(productDTO.getThuongHieu());
        product.setCongDung(productDTO.getCongDung());
        product.setDangBaoChe(productDTO.getDangBaoChe());
        product.setQuyCach(productDTO.getQuyCach());
        product.setXuatXuThuongHieu(productDTO.getXuatXuThuongHieu());
        product.setNhaSanXuat(productDTO.getNhaSanXuat());
        product.setNuocSanXuat(productDTO.getNuocSanXuat());
        product.setThanhPhan(productDTO.getThanhPhan());
        product.setMoTaNgan(productDTO.getMoTaNgan());
        product.setSoDangKy(productDTO.getSoDangKy());

        if (productDTO.getDanhMucId() != 0) {
            DanhMuc danhMuc = danhMucRepository.findById(productDTO.getDanhMucId())
                    .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
            product.setDanhMuc(danhMuc);
        }

        if (productDTO.getDoiTuongId() != 0) {
            DoiTuong doiTuong = doiTuongRepository.findById(productDTO.getDoiTuongId())
                    .orElseThrow(() -> new RuntimeException("Đối tượng không tồn tại"));
            product.setDoiTuong(doiTuong);
        }

        product = productRepository.save(product);

        List<ProductImage> images = new ArrayList<>();
        boolean hasMainImage = false;
        for (ProductDTO.ImageDTO imageDTO : productDTO.getImages()) {
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl(imageDTO.getImageUrl());
            image.setImageOrder(imageDTO.getImageOrder());
            if (imageDTO.isMain() && !hasMainImage) {
                image.setMain(true);
                hasMainImage = true;
            } else {
                image.setMain(false);
            }
            images.add(image);
        }
        if (!hasMainImage && !images.isEmpty()) {
            images.get(0).setMain(true);
        }
        productImageRepository.saveAll(images);

        List<DonViTinh> donViTinhList = new ArrayList<>();
        for (ProductDTO.DonViTinhDTO dvtDTO : productDTO.getDonViTinhList()) {
            DonViTinh dvt = new DonViTinh();
            dvt.setProduct(product);
            dvt.setDonViTinh(dvtDTO.getDonViTinh());
            dvt.setGia(BigDecimal.valueOf(dvtDTO.getGia()));
            dvt.setDiscount(dvtDTO.getDiscount() != null ? BigDecimal.valueOf(dvtDTO.getDiscount()) : null);
            dvt.setGhiChu(dvtDTO.getGhiChu());
            donViTinhList.add(dvt);
        }
        donViTinhRepository.saveAll(donViTinhList);
        product.setDonViTinhList(donViTinhList);

        ChiTietSanPham chiTiet = new ChiTietSanPham();
        chiTiet.setProduct(product);
        chiTiet.setProductId(product.getId());
        chiTiet.setMoTaChiTiet(productDTO.getChiTietSanPham().getMoTaChiTiet());
        chiTiet.setThanhPhanChiTiet(productDTO.getChiTietSanPham().getThanhPhanChiTiet());
        chiTiet.setCongDungChiTiet(productDTO.getChiTietSanPham().getCongDungChiTiet());
        chiTiet.setCachDungChiTiet(productDTO.getChiTietSanPham().getCachDungChiTiet());
        chiTiet.setTacDungPhu(productDTO.getChiTietSanPham().getTacDungPhu());
        chiTiet.setLuuY(productDTO.getChiTietSanPham().getLuuY());
        chiTiet.setBaoQuan(productDTO.getChiTietSanPham().getBaoQuan());
        chiTietSanPhamRepository.save(chiTiet);

        return product;
    }

    @Transactional
    public ProductDTO updateProduct(Integer id, ProductDTO productDTO) {
        Optional<Product> productOptional = productRepository.findById(id);
        if (!productOptional.isPresent()) {
            throw new RuntimeException("Sản phẩm không tồn tại!");
        }

        Product product = productOptional.get();

        // Cập nhật thông tin cơ bản
        product.setTenSanPham(productDTO.getTenSanPham());
        product.setThuongHieu(productDTO.getThuongHieu());
        product.setCongDung(productDTO.getCongDung());
        product.setDangBaoChe(productDTO.getDangBaoChe());
        product.setQuyCach(productDTO.getQuyCach());
        product.setXuatXuThuongHieu(productDTO.getXuatXuThuongHieu());
        product.setNhaSanXuat(productDTO.getNhaSanXuat());
        product.setNuocSanXuat(productDTO.getNuocSanXuat());
        product.setThanhPhan(productDTO.getThanhPhan());
        product.setMoTaNgan(productDTO.getMoTaNgan());
        product.setSoDangKy(productDTO.getSoDangKy());

        // Cập nhật DanhMuc
        if (productDTO.getDanhMucId() != 0) {
            DanhMuc danhMuc = danhMucRepository.findById(productDTO.getDanhMucId())
                    .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
            product.setDanhMuc(danhMuc);
        } else {
            throw new RuntimeException("Danh mục là trường bắt buộc!");
        }

        // Cập nhật DoiTuong
        if (productDTO.getDoiTuongId() != 0) {
            DoiTuong doiTuong = doiTuongRepository.findById(productDTO.getDoiTuongId())
                    .orElseThrow(() -> new RuntimeException("Đối tượng không tồn tại"));
            product.setDoiTuong(doiTuong);
        } else {
            product.setDoiTuong(null); // Cho phép null nếu không bắt buộc
        }

        // Lưu thông tin cơ bản
        product = productRepository.save(product);

        // Cập nhật hình ảnh
        List<ProductImage> existingImages = productImageRepository.findByProductId(id);
        productImageRepository.deleteAll(existingImages); // Xóa toàn bộ hình ảnh cũ
        List<ProductImage> newImages = new ArrayList<>();
        boolean hasMainImage = false;
        for (ProductDTO.ImageDTO imageDTO : productDTO.getImages()) {
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl(imageDTO.getImageUrl());
            image.setImageOrder(imageDTO.getImageOrder());
            if (imageDTO.isMain() && !hasMainImage) {
                image.setMain(true);
                hasMainImage = true;
            } else {
                image.setMain(false);
            }
            newImages.add(image);
        }
        if (!hasMainImage && !newImages.isEmpty()) {
            newImages.get(0).setMain(true); // Đặt ảnh đầu tiên làm ảnh chính nếu chưa có
        }
        if (newImages.isEmpty()) {
            throw new RuntimeException("Phải có ít nhất một hình ảnh!");
        }
        productImageRepository.saveAll(newImages);

        // Cập nhật đơn vị tính
        List<DonViTinh> existingUnits = donViTinhRepository.findByProductId(id);
        donViTinhRepository.deleteAll(existingUnits); // Xóa toàn bộ đơn vị tính cũ
        List<DonViTinh> newUnits = new ArrayList<>();
        for (ProductDTO.DonViTinhDTO dvtDTO : productDTO.getDonViTinhList()) {
            DonViTinh dvt = new DonViTinh();
            dvt.setProduct(product);
            dvt.setDonViTinh(dvtDTO.getDonViTinh());
            dvt.setGia(BigDecimal.valueOf(dvtDTO.getGia()));
            dvt.setDiscount(dvtDTO.getDiscount() != null ? BigDecimal.valueOf(dvtDTO.getDiscount()) : null);
            dvt.setGhiChu(dvtDTO.getGhiChu());
            newUnits.add(dvt);
        }
        if (newUnits.isEmpty()) {
            throw new RuntimeException("Phải có ít nhất một đơn vị tính!");
        }
        donViTinhRepository.saveAll(newUnits);
        product.setDonViTinhList(newUnits);

        // Cập nhật chi tiết sản phẩm
        Optional<ChiTietSanPham> chiTietOptional = chiTietSanPhamRepository.findByProductId(id);
        ChiTietSanPham chiTiet;
        if (chiTietOptional.isPresent()) {
            chiTiet = chiTietOptional.get();
        } else {
            chiTiet = new ChiTietSanPham();
            chiTiet.setProduct(product);
            chiTiet.setProductId(product.getId());
        }
        chiTiet.setMoTaChiTiet(productDTO.getChiTietSanPham().getMoTaChiTiet());
        chiTiet.setThanhPhanChiTiet(productDTO.getChiTietSanPham().getThanhPhanChiTiet());
        chiTiet.setCongDungChiTiet(productDTO.getChiTietSanPham().getCongDungChiTiet());
        chiTiet.setCachDungChiTiet(productDTO.getChiTietSanPham().getCachDungChiTiet());
        chiTiet.setTacDungPhu(productDTO.getChiTietSanPham().getTacDungPhu());
        chiTiet.setLuuY(productDTO.getChiTietSanPham().getLuuY());
        chiTiet.setBaoQuan(productDTO.getChiTietSanPham().getBaoQuan());
        chiTietSanPhamRepository.save(chiTiet);

        // Trả về ProductDTO đã cập nhật
        return getProductDetail(id);
    }

    public List<Inventory> getInventoryByProductId(Integer productId) {
        return inventoryRepository.findByProductId(productId);
    }

    public List<Kho> getAllKho() {
        return khoRepository.findAll();
    }

    @Transactional
    public Kho addKho(Kho kho) {
        if (khoRepository.existsByMaKho(kho.getMaKho())) {
            throw new RuntimeException("Mã kho '" + kho.getMaKho() + "' đã tồn tại!");
        }
        if (kho.getNgayKhoiTao() == null) {
            kho.setNgayKhoiTao(LocalDate.now());
        }
        return khoRepository.save(kho);
    }

    public List<NhaCungCap> getAllNhaCungCap() {
        return nhaCungCapRepository.findAll();
    }



    // Tổng số sản phẩm
    public long getTotalProducts() {
        return productRepository.count();
    }

    // Tổng số đơn hàng
    public long getTotalOrders() {
        return ordersRepository.count();
    }

    // Tổng số người dùng
    public long getTotalUsers() {
        return userRepository.count();
    }

    // Số lượng đánh giá mới (trong 7 ngày gần nhất)
    public long getNewReviews() {
        java.time.LocalDateTime sevenDaysAgo = java.time.LocalDateTime.now().minusDays(7);
        return danhGiaRepository.countByCreatedAtAfter(sevenDaysAgo);
    }

    // Thống kê đơn hàng theo trạng thái
    public Map<String, Long> getOrderStats() {
        List<Orders> orders = ordersRepository.findAll();
        return orders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getStatus().toString(),
                        Collectors.counting()
                ));
    }

    // Thống kê sản phẩm theo danh mục
    public Map<String, Long> getProductByCategoryStats() {
        List<Product> products = productRepository.findAll();
        Map<String, Long> stats = new HashMap<>();
        for (Product product : products) {
            String categoryName = product.getDanhMuc() != null ? product.getDanhMuc().getTenDanhMuc() : "Không xác định";
            stats.put(categoryName, stats.getOrDefault(categoryName, 0L) + 1);
        }
        return stats;
    }


    // Lấy danh sách tất cả người dùng
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Lấy người dùng theo ID
    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    // Xóa người dùng
    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Người dùng không tồn tại!");
        }
        userRepository.deleteById(id);
    }

//    public List<Orders> getOrdersByUserId(UUID userId) {
//        return ordersRepository.findByUserId(userId);
//    }



    // Cập nhật thông tin người dùng
    @Transactional
    public User updateUser(UUID id, User updatedUser) {
        Optional<User> userOptional = userRepository.findById(id);
        if (!userOptional.isPresent()) {
            throw new RuntimeException("Người dùng không tồn tại!");
        }

        User user = userOptional.get();
        // Cập nhật các trường
        if (updatedUser.getName() != null) {
            user.setName(updatedUser.getName());
        }
        if (updatedUser.getEmail() != null) {
            user.setEmail(updatedUser.getEmail());
        }
        if (updatedUser.getPhoneNumber() != null) {
            user.setPhoneNumber(updatedUser.getPhoneNumber());
        }
        if (updatedUser.getAddress() != null) {
            user.setAddress(updatedUser.getAddress());
        }
        if (updatedUser.getDateOfBirth() != null) {
            user.setDateOfBirth(updatedUser.getDateOfBirth());
        }
        if (updatedUser.getRole() != null) {
            user.setRole(updatedUser.getRole());
        }
        if (updatedUser.getPicture() != null) {
            user.setPicture(updatedUser.getPicture());
        }

        return userRepository.save(user);
    }


    public List<Orders> getOrdersByUserId(UUID userId) {
        return ordersRepository.findByUserIdWithCart(userId);
    }

    // Lấy chi tiết đơn hàng theo ID
    public Orders getOrderDetails(int orderId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại!"));

        // Lấy danh sách sản phẩm từ Cart
        if (order.getCart() != null) {
            List<CartItem> cartItems = cartItemRepository.findByCart(order.getCart());
            order.getCart().setCartItems(cartItems);
        }

        return order;
    }

    public List<Orders> getAllOrders(int page, int size, String search, String status) {
        Pageable pageable = PageRequest.of(page - 1, size); // Page bắt đầu từ 0 trong Spring Data
        Page<Orders> ordersPage;

        // Xây dựng điều kiện tìm kiếm và lọc
        if (search != null && !search.isEmpty() && status != null && !status.isEmpty()) {
            // Tìm kiếm theo search (có thể là ID hoặc thông tin khách hàng) và lọc theo status
            ordersPage = ordersRepository.findByIdContainingAndStatus(search, status, pageable);
        } else if (search != null && !search.isEmpty()) {
            // Chỉ tìm kiếm
            ordersPage = ordersRepository.findByIdContaining(search, pageable);
        } else if (status != null && !status.isEmpty()) {
            // Chỉ lọc theo trạng thái
            ordersPage = ordersRepository.findByStatus(status, pageable);
        } else {
            // Lấy tất cả đơn hàng
            ordersPage = ordersRepository.findAll(pageable);
        }

        return ordersPage.getContent();
    }



    public List<OrderItem> getOrderItems(int orderId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại!"));
        return orderItemRepository.findByOrder(order);
    }

    private InventoryResponseDTO mapToInventoryResponseDTO(Inventory inventory) {
        InventoryResponseDTO dto = new InventoryResponseDTO();
        dto.setId(inventory.getId());
        dto.setBatchNumber(inventory.getBatchNumber());
        dto.setQuantity(inventory.getQuantity());
        dto.setManufactureDate(inventory.getManufactureDate());
        dto.setExpiryDate(inventory.getExpiryDate());
        dto.setStorageLocation(inventory.getStorageLocation());
        dto.setGhichu(inventory.getGhichu());
        dto.setCreatedAt(inventory.getCreatedAt());
        dto.setUpdatedAt(inventory.getUpdatedAt());

        // Ánh xạ Kho
        if (inventory.getKho() != null) {
            InventoryResponseDTO.KhoDTO khoDTO = new InventoryResponseDTO.KhoDTO();
            khoDTO.setId(inventory.getKho().getId());
            khoDTO.setMaKho(inventory.getKho().getMaKho());
            khoDTO.setTenKho(inventory.getKho().getTenKho());
            dto.setKho(khoDTO);
        }

        // Ánh xạ Product
        if (inventory.getProduct() != null) {
            InventoryResponseDTO.ProductDTO productDTO = new InventoryResponseDTO.ProductDTO();
            productDTO.setId(inventory.getProduct().getId());
            productDTO.setTenSanPham(inventory.getProduct().getTenSanPham());
            dto.setProduct(productDTO);
        }

        // Ánh xạ Supplier
        if (inventory.getSupplier() != null) {
            InventoryResponseDTO.SupplierDTO supplierDTO = new InventoryResponseDTO.SupplierDTO();
            supplierDTO.setId(inventory.getSupplier().getId());
            supplierDTO.setTenNhaCungCap(inventory.getSupplier().getTenNhaCungCap());
            dto.setSupplier(supplierDTO);
        }

        return dto;
    }

    public List<InventoryResponseDTO> getAllInventory() {
        List<Inventory> inventories = inventoryRepository.findAllWithDetails();
        return inventories.stream()
                .map(this::mapToInventoryResponseDTO)
                .collect(Collectors.toList());
    }


    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    private void recordInventoryTransaction(Inventory inventory, InventoryTransaction.TransactionType type,
                                            Integer quantity, String reason, Orders order, User createdBy) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setInventory(inventory);
        transaction.setKho(inventory.getKho());
        transaction.setProduct(inventory.getProduct());
        transaction.setSupplier(inventory.getSupplier());
        transaction.setBatchNumber(inventory.getBatchNumber());
        transaction.setTransactionType(type);
        transaction.setQuantity(quantity);
        transaction.setReason(reason);
        transaction.setOrder(order);
        transaction.setCreatedBy(createdBy);
        inventoryTransactionRepository.save(transaction);
    }

    @Transactional
    public Inventory addInventory(InventoryResponseDTO inventoryDTO) {
        Kho kho = khoRepository.findById(inventoryDTO.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Kho không tồn tại"));
        Product product = productRepository.findById(inventoryDTO.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
        NhaCungCap supplier = nhaCungCapRepository.findById(inventoryDTO.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Nhà cung cấp không tồn tại"));

        if (inventoryRepository.existsByKhoIdAndBatchNumber(inventoryDTO.getWarehouseId(), inventoryDTO.getBatchNumber())) {
            throw new RuntimeException("Số lô '" + inventoryDTO.getBatchNumber() + "' đã tồn tại trong kho!");
        }

        Inventory inventory = new Inventory();
        inventory.setKho(kho);
        inventory.setProduct(product);
        inventory.setBatchNumber(inventoryDTO.getBatchNumber());
        inventory.setQuantity(inventoryDTO.getQuantity());
        inventory.setManufactureDate(inventoryDTO.getManufactureDate());
        inventory.setExpiryDate(inventoryDTO.getExpiryDate());
        inventory.setStorageLocation(inventoryDTO.getStorageLocation());
        inventory.setSupplier(supplier);
        inventory.setGhichu(inventoryDTO.getNote());

        Inventory savedInventory = inventoryRepository.save(inventory);

        // Ghi giao dịch nhập kho
        recordInventoryTransaction(savedInventory, InventoryTransaction.TransactionType.NHAP_KHO,
                inventoryDTO.getQuantity(), "Nhập lô hàng mới", null, null);

        return savedInventory;
    }

    @Transactional
    public void processOrderInventory(Integer orderId, User user) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại!"));

        if (!order.getStatus().equals("completed")) {
            throw new RuntimeException("Đơn hàng chưa được xác nhận!");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrder(order);
        for (OrderItem item : orderItems) {
            Integer productId = item.getProduct().getId();
            Integer quantity = item.getQuantity();

            // Tìm bản ghi inventory phù hợp (ưu tiên lô gần hết hạn)
            List<Inventory> inventories = inventoryRepository.findByProductId(productId);
            inventories.sort(Comparator.comparing(Inventory::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder())));

            int remainingQuantity = quantity;
            for (Inventory inventory : inventories) {
                if (remainingQuantity <= 0) break;

                int availableQuantity = inventory.getQuantity();
                int deductQuantity = Math.min(remainingQuantity, availableQuantity);

                inventory.setQuantity(availableQuantity - deductQuantity);
                inventoryRepository.save(inventory);

                // Ghi giao dịch xuất kho
                recordInventoryTransaction(inventory, InventoryTransaction.TransactionType.XUAT_KHO,
                        -deductQuantity, "Xuất kho cho đơn hàng #" + orderId, order, user);

                remainingQuantity -= deductQuantity;
            }

            if (remainingQuantity > 0) {
                throw new RuntimeException("Không đủ tồn kho cho sản phẩm ID: " + productId);
            }
        }
    }

    public List<InventoryTransactionDTO> getInventoryTransactions(Integer khoId, Integer productId,
                                                                  LocalDateTime startDate, LocalDateTime endDate) {
        List<InventoryTransaction> transactions = inventoryTransactionRepository.findTransactions(khoId, productId, startDate, endDate);
        return transactions.stream()
                .map(this::mapToInventoryTransactionDTO)
                .collect(Collectors.toList());
    }

    private InventoryTransactionDTO mapToInventoryTransactionDTO(InventoryTransaction transaction) {
        InventoryTransactionDTO dto = new InventoryTransactionDTO();
        dto.setId(transaction.getId());
        dto.setInventoryId(transaction.getInventory().getId());
        dto.setKho(new InventoryResponseDTO.KhoDTO());
        dto.getKho().setId(transaction.getKho().getId());
        dto.getKho().setMaKho(transaction.getKho().getMaKho());
        dto.getKho().setTenKho(transaction.getKho().getTenKho());
        dto.setProduct(new InventoryResponseDTO.ProductDTO());
        dto.getProduct().setId(transaction.getProduct().getId());
        dto.getProduct().setTenSanPham(transaction.getProduct().getTenSanPham());
        if (transaction.getSupplier() != null) {
            dto.setSupplier(new InventoryResponseDTO.SupplierDTO());
            dto.getSupplier().setId(transaction.getSupplier().getId());
            dto.getSupplier().setTenNhaCungCap(transaction.getSupplier().getTenNhaCungCap());
        }
        dto.setBatchNumber(transaction.getBatchNumber());
        dto.setTransactionType(transaction.getTransactionType().toString());
        dto.setQuantity(transaction.getQuantity());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setReason(transaction.getReason());
        dto.setOrderId(transaction.getOrder() != null ? transaction.getOrder().getId() : null);
        dto.setCreatedBy(transaction.getCreatedBy() != null ? transaction.getCreatedBy().getName() : null);
        dto.setCreatedAt(transaction.getCreatedAt());
        return dto;
    }

}