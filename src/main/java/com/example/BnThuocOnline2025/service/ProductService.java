package com.example.BnThuocOnline2025.service;

import com.example.BnThuocOnline2025.model.*;
import com.example.BnThuocOnline2025.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ChiTietSanPhamRepository chiTietSanPhamRepository;

    @Autowired
    private CauHoiLienQuanRepository cauHoiLienQuanRepository;

    @Autowired
    private DoiTuongRepository doiTuongRepository;

    @Autowired
    private DonViTinhRepository donViTinhRepository;

    @Autowired
    private ProductReviewRepository productReviewRepository;

    private static final int PAGE_SIZE = 12;

    public Page<Product> getProducts(int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Page<Product> productPage = productRepository.findAll(pageable);
        productPage.getContent().forEach(this::loadProductDetails);
        return productPage;
    }

    public Product getProductById(int id) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            loadProductDetails(product);
            return product;
        }
        return null;
    }

    public List<Product> getProductsByDoiTuongId(int doiTuongId) {
        List<Product> products = productRepository.findByDoiTuongId(doiTuongId);
        products.forEach(this::loadProductDetails);
        return products;
    }

    public List<DoiTuong> getAllDoiTuong() {
        return doiTuongRepository.findAll();
    }

    public List<ProductImage> getProductImages(int productId) {
        return productImageRepository.findByProductId(productId);
    }

    public ChiTietSanPham getChiTietSanPhamByProductId(int productId) {
        Optional<ChiTietSanPham> chiTietOpt = chiTietSanPhamRepository.findByProductId(productId);
        return chiTietOpt.orElse(null);
    }

    public List<CauHoiLienQuan> getCauHoiLienQuanByProductId(int productId) {
        return cauHoiLienQuanRepository.findByProductId(productId);
    }

    public List<Product> getRelatedProducts(int productId, int limit) {
        Product currentProduct = getProductById(productId);
        if (currentProduct == null || currentProduct.getDanhMuc() == null) {
            return List.of();
        }

        List<Product> relatedProducts = productRepository.findByDanhMucId(currentProduct.getDanhMuc().getId());
        return relatedProducts.stream()
                .filter(p -> p.getId() != productId)
                .peek(this::loadProductDetails)
                .limit(limit)
                .collect(Collectors.toList());
    }

//    private void loadProductDetails(Product product) {
//        Optional<ProductImage> mainImage = productImageRepository.findByProductIdAndIsMainTrue(product.getId());
//        mainImage.ifPresent(image -> product.setMainImageUrl(image.getImageUrl()));
//
//        List<DonViTinh> donViTinhList = donViTinhRepository.findByProductId(product.getId());
//        product.setDonViTinhList(donViTinhList);
//    }



    public BigDecimal calculateDiscountedPrice(BigDecimal gia, BigDecimal discount) {
        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            return gia.multiply(BigDecimal.ONE.subtract(discount.divide(BigDecimal.valueOf(100))));
        }
        return gia;
    }

    public DonViTinh getDonViTinhById(int productId, int donViTinhId) {
        return donViTinhRepository.findById(donViTinhId)
                .filter(dvt -> dvt.getProduct().getId() == productId)
                .orElse(null);
    }

    public void loadProductDetails(Product product) {
        Optional<ProductImage> mainImage = productImageRepository.findByProductIdAndIsMainTrue(product.getId());
        mainImage.ifPresent(image -> product.setMainImageUrl(image.getImageUrl()));

        List<DonViTinh> donViTinhList = donViTinhRepository.findByProductId(product.getId());
        product.setDonViTinhList(donViTinhList);
    }




    // Lấy danh sách đánh giá theo productId
    public List<ProductReview> getProductReviews(int productId) {
        return productReviewRepository.findByProductId(productId);
    }

    // Lấy đánh giá theo số sao
    public List<ProductReview> getProductReviewsByRating(int productId, float rating) {
        return productReviewRepository.findByProductIdAndRating(productId, rating);
    }

    // Lấy trung bình số sao
    public Double getAverageRating(int productId) {
        Double avgRating = productReviewRepository.findAverageRatingByProductId(productId);
        return avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0;
    }

    // Đếm số lượng đánh giá
    public long getReviewCount(int productId) {
        return productReviewRepository.countByProductId(productId);
    }

    // Lưu đánh giá
    public ProductReview saveProductReview(ProductReview review) {
        return productReviewRepository.save(review);
    }

    // Lấy số lượng đánh giá theo từng mức sao
    public Map<Integer, Long> getReviewCountByRating(int productId) {
        Map<Integer, Long> ratingCounts = new HashMap<>();
        List<ProductReview> reviews = productReviewRepository.findByProductId(productId);
        for (int i = 1; i <= 5; i++) {
            final int rating = i;
            long count = reviews.stream()
                    .filter(review -> Math.round(review.getRating()) == rating)
                    .count();
            ratingCounts.put(i, count);
        }
        return ratingCounts;
    }

    public List<Product> getProductsByDanhMucId(int danhMucId) {
        List<Product> products = productRepository.findByDanhMucId(danhMucId);
        products.forEach(this::loadProductDetails);
        return products;
    }


    public List<Product> searchProductsByName(String query) {
        List<Product> products = productRepository.findByTenSanPhamContainingIgnoreCase(query);
        products.forEach(this::loadProductDetails);
        return products;
    }
}