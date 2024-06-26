package com.min204.coseproject.user.service;

import com.min204.coseproject.constant.ErrorCode;
import com.min204.coseproject.content.repository.ContentRepository;
import com.min204.coseproject.course.dto.PlaceDto;
import com.min204.coseproject.course.repository.CourseRepository;
import com.min204.coseproject.exception.BusinessLogicException;
import com.min204.coseproject.follow.repository.FollowRepository;
import com.min204.coseproject.redis.RedisUtil;
import com.min204.coseproject.user.dao.UserDao;
import com.min204.coseproject.user.dao.UserPhotoDao;
import com.min204.coseproject.user.dto.req.UserRequestDto;
import com.min204.coseproject.user.dto.res.UserProfileResponseDto;
import com.min204.coseproject.user.entity.User;
import com.min204.coseproject.user.entity.UserPhoto;
import com.min204.coseproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserDao userDao;
    private final UserPhotoDao userPhotoDao;
    private final UserRepository userRepository;
    private final RedisUtil redisUtil;
    private final PasswordEncoder passwordEncoder;
    private final ContentRepository contentRepository;
    private final FollowRepository followRepository;
    private final CourseRepository courseRepository;


    private static final String DEFAULT_IMAGE_PATH = "../defaultImage.svg";

    private void validateCurrentUser(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.getName().equals(userId.toString())) {
            throw new BusinessLogicException(ErrorCode.UNAUTHORIZED);
        }
    }

    @Override
    public User find(Long userId) {
        return userDao.find(userId);
    }

    @Override
    public UserProfileResponseDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.USER_NOT_FOUND));
        return buildUserProfileResponse(user);
    }

    private UserProfileResponseDto buildUserProfileResponse(User user) {
        int postCount = contentRepository.countByUser(user);
        int followerCount = followRepository.countByFollowee(user);
        int followingCount = followRepository.countByFollower(user);
        int courseCount = courseRepository.countByUser(user);

        List<UserProfileResponseDto.ContentDto> posts = contentRepository.findAllByUser(user).stream()
                .map(content -> UserProfileResponseDto.ContentDto.builder()
                        .contentId(content.getContentId())
                        .courses(content.getCourses().stream()
                                .map(course -> UserProfileResponseDto.CourseDto.builder()
                                        .courseId(course.getCourseId())
                                        .courseName(course.getCourseName())
                                        .places(course.getPlaces().stream()
                                                .map(place -> PlaceDto.builder()
                                                        .placeName(place.getPlaceName())
                                                        .address(place.getAddress())
                                                        .placeUrl(place.getPlaceUrl())
                                                        .categoryGroupName(place.getCategoryGroupName())
                                                        .x(place.getX())
                                                        .y(place.getY())
                                                        .build())
                                                .collect(Collectors.toList()))
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        List<UserProfileResponseDto.UserDto> followers = followRepository.findByFollowee(user).stream()
                .map(follow -> UserProfileResponseDto.UserDto.builder()
                        .userId(follow.getFollower().getUserId())
                        .email(follow.getFollower().getEmail())
                        .build())
                .collect(Collectors.toList());

        List<UserProfileResponseDto.UserDto> following = followRepository.findByFollower(user).stream()
                .map(follow -> UserProfileResponseDto.UserDto.builder()
                        .userId(follow.getFollowee().getUserId())
                        .email(follow.getFollowee().getEmail())
                        .build())
                .collect(Collectors.toList());

        List<UserProfileResponseDto.CourseDto> courses = courseRepository.findAllByUser(user).stream()
                .map(course -> UserProfileResponseDto.CourseDto.builder()
                        .courseId(course.getCourseId())
                        .courseName(course.getCourseName())
                        .places(course.getPlaces().stream()
                                .map(place -> PlaceDto.builder()
                                        .placeName(place.getPlaceName())
                                        .address(place.getAddress())
                                        .placeUrl(place.getPlaceUrl())
                                        .categoryGroupName(place.getCategoryGroupName())
                                        .x(place.getX())
                                        .y(place.getY())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        String profileImagePath = user.getUserPhoto() != null ? user.getUserPhoto().getFilePath() : DEFAULT_IMAGE_PATH;

        return UserProfileResponseDto.builder()
                .nickname(user.getNickname())
                .postCount(postCount)
                .posts(posts)
                .followerCount(followerCount)
                .followers(followers)
                .followingCount(followingCount)
                .following(following)
                .profileImagePath(profileImagePath)
                .courseCount(courseCount)
                .courses(courses)
                .build();
    }

    @Override
    public List<User> findAll() {
        return userDao.findAll();
    }

    @Override
    public Optional<User> update(UserRequestDto userRequestDto) {
        User user = userDao.findById(userRequestDto.getUserId())
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.USER_NOT_FOUND));
        user.changeInfo(userRequestDto);
        return Optional.of(user);
    }

//    @Override
//    public void delete(Long userId) {
//        List<Scrap> scraps = scrapRepository.findByUserId(userId);
//        scrapRepository.deleteAll(scraps);
//
//        // 사용자 관련 컨텐츠 삭제
//        List<Content> contents = contentRepository.findByUserId(userId);
//        for (Content content : contents) {
//            // 컨텐츠 관련 코스 삭제
//            List<Course> courses = courseRepository.findByContentId(content.getContentId());
//            courseRepository.deleteAll(courses);
//        }
//        contentRepository.deleteAll(contents);
//
//        // 사용자 관련 팔로우 삭제
//        followRepository.deleteByFollowerUserIdOrFolloweeUserId(userId, userId);
//
//        // 사용자 삭제
//        userRepository.deleteById(userId);
//    }



    @Override
    public List<UserPhoto> findUserPhoto(Long userId) {
        User user = userDao.find(userId);
        return userPhotoDao.findUserPhotosByUser(user);
    }



    @Override
    public User getLoginMember() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new BusinessLogicException(ErrorCode.USER_NOT_FOUND));
        } else {
            throw new BusinessLogicException(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Override
    public boolean resetPassword(Long userId, String newPassword) {
        User user = userDao.find(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        userDao.save(user);
        redisUtil.deleteData(user.getEmail());
        return true;
    }

    @Override
    public String checkUserPlatform(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.USER_NOT_FOUND));
        return user.getLoginType().name();
    }

    @Override
    public Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::getUserId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.USER_NOT_FOUND));
    }
}