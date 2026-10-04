package com.county.coldchain.domain.entity;

import com.county.coldchain.domain.BaseEntity;
import com.county.coldchain.domain.DomainEnums.UserRole;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** 平台用户：演示环境下密码为明文（README 中列出测试账号）。 */
@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class SysUser extends BaseEntity {
    private String username;
    private String password;
    private String displayName;
    @Enumerated(EnumType.STRING)
    private UserRole role;
    private Boolean enabled = true;
}
