package io.github.liangxin233666.mfl.config; // 使用你自己的包名

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public abstract class UserMixin {


    @JsonCreator
    public UserMixin(@JsonProperty("username") String username,
                     @JsonProperty("password") String password,
                     @JsonProperty("enabled") boolean enabled,
                     @JsonProperty("accountNonExpired") boolean accountNonExpired,
                     @JsonProperty("credentialsNonExpired") boolean credentialsNonExpired,
                     @JsonProperty("accountNonLocked") boolean accountNonLocked,
                     @JsonProperty("authorities") Collection<? extends GrantedAuthority> authorities) {
    }
}