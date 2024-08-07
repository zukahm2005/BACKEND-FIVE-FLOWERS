package com.example.backendfiveflowers.service;

import com.example.backendfiveflowers.entity.Blog;
import com.example.backendfiveflowers.entity.UserInfo;
import com.example.backendfiveflowers.model.Article;
import com.example.backendfiveflowers.model.NewsResponse;
import com.example.backendfiveflowers.repository.BlogRepository;
import com.example.backendfiveflowers.repository.UserInfoRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class BlogService {

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "Bicycles", "bicycle racing", "bicycle maintenance", "bicycle accessories", "effects of cycling"
    ));

    public BlogService() {
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public Blog addBlog(Blog blog) {
        String username = getCurrentUsername();
        Optional<UserInfo> userInfoOptional = userInfoRepository.findByUserName(username);
        if (userInfoOptional.isPresent()) {
            blog.setAuthor(userInfoOptional.get());
        } else {
            throw new RuntimeException("User not found");
        }

        return blogRepository.save(blog);
    }

    public String storeFile(MultipartFile file) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public Blog updateBlog(Integer id, Blog blog) {
        Optional<Blog> existingBlog = blogRepository.findById(id);
        if (existingBlog.isPresent()) {
            Blog updatedBlog = existingBlog.get();
            updatedBlog.setTitle(blog.getTitle());
            updatedBlog.setContent(blog.getContent());
            return blogRepository.save(updatedBlog);
        } else {
            throw new RuntimeException("Blog not found");
        }
    }

    public void deleteBlog(Integer id) {
        blogRepository.deleteById(id);
    }

    public Optional<Blog> getBlogById(Integer id) {
        return blogRepository.findById(id);
    }

    public Page<Blog> getAllBlogs(Pageable pageable) {
        return blogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else {
            return principal.toString();
        }
    }

    public List<Blog> fetchBicycleNews() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://newsapi.org/v2/everything?q=" + String.join(" OR ", KEYWORDS) + "&apiKey=d8a0a2831ea443a1b746f7cdcd0c8e1b";
        NewsResponse response = restTemplate.getForObject(url, NewsResponse.class);

        if (response != null && response.getArticles() != null) {
            response.getArticles().forEach(article -> {
                String fullContent = fetchFullContentFromUrl(article.getUrl());

                Blog blog = new Blog();
                blog.setTitle(article.getTitle());
                blog.setContent(fullContent);
                blog.setImageUrl(article.getUrlToImage()); // Lưu URL ảnh đại diện
                blog.setCreatedAt(LocalDateTime.now());
                blog.setUpdatedAt(LocalDateTime.now());

                String username = getCurrentUsername();
                Optional<UserInfo> userInfoOptional = userInfoRepository.findByUserName(username);
                userInfoOptional.ifPresent(blog::setAuthor);

                blogRepository.save(blog);
            });
        }
        return blogRepository.findAll();
    }

    public NewsResponse searchArticles(String keyword) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://newsapi.org/v2/everything?q=" + keyword + "&sortBy=publishedAt&apiKey=d8a0a2831ea443a1b746f7cdcd0c8e1b";
        return restTemplate.getForObject(url, NewsResponse.class);
    }

    public Article processArticle(Article article) {
        String fullContent = fetchFullContentFromUrl(article.getUrl());

        article.setContent(fullContent);

        return article;
    }

    public Blog autoPostBlog() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://newsapi.org/v2/everything?q=" + String.join(" OR ", KEYWORDS) + "&sortBy=publishedAt&apiKey=d8a0a2831ea443a1b746f7cdcd0c8e1b";
        NewsResponse response = restTemplate.getForObject(url, NewsResponse.class);

        if (response != null && response.getArticles() != null && !response.getArticles().isEmpty()) {
            for (Article article : response.getArticles()) {
                // Kiểm tra trùng lặp
                if (!blogRepository.existsByTitle(article.getTitle())) {
                    String fullContent = fetchFullContentFromUrl(article.getUrl());

                    Blog blog = new Blog();
                    blog.setTitle(article.getTitle());
                    blog.setContent(fullContent);
                    blog.setImageUrl(article.getUrlToImage()); // Lưu URL ảnh đại diện
                    blog.setCreatedAt(LocalDateTime.now());
                    blog.setUpdatedAt(LocalDateTime.now());

                    String username = getCurrentUsername();
                    Optional<UserInfo> userInfoOptional = userInfoRepository.findByUserName(username);
                    userInfoOptional.ifPresent(blog::setAuthor);

                    return blogRepository.save(blog);
                }
            }
        }

        throw new RuntimeException("No new articles found");
    }

    private String fetchFullContentFromUrl(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            Elements elements = doc.select("article, article p, article img");
            if (elements.isEmpty()) {
                elements = doc.select("div.content, div.content p, div.content img");
            }

            StringBuilder content = new StringBuilder();
            for (Element element : elements) {
                if (element.tagName().equals("img")) {
                    String imageUrl = element.absUrl("src");
                    content.append("<img src=\"").append(imageUrl).append("\" alt=\"Image\"><br>");
                } else {
                    content.append(element.text()).append("<br><br>");
                }
            }
            return content.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "Unable to fetch full content.";
        }
    }
}
