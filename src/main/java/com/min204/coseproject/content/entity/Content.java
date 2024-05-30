package com.min204.coseproject.content.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.min204.coseproject.audit.Auditable;
import com.min204.coseproject.comment.entity.Comment;
import com.min204.coseproject.heart.entity.Heart;
import com.min204.coseproject.course.entity.Course;
import com.min204.coseproject.user.entity.User;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "contents")
public class Content extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "content_id")
    private Long contentId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int viewCount = 0;

    @Column(nullable = false)
    private int heartCount = 0;

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Course> courses = new ArrayList<>();

    @OneToMany(mappedBy = "content", cascade = CascadeType.REMOVE)
    private List<Heart> hearts = new ArrayList<>();

    @OneToMany(mappedBy = "content", cascade = CascadeType.REMOVE)
    private List<Comment> comments = new ArrayList<>();

    @ManyToOne(optional = true, fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinColumn(name = "user_id")
    private User user;

    public void addHeart(Heart heart) {
        hearts.add(heart);
    }

    public void addCourse(Course course) {
        courses.add(course);
        course.setContent(this);
    }
}
