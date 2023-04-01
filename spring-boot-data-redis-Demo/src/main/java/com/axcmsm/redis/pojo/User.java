package com.axcmsm.redis.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * this class is for Axcmsm
 *
 * @author 须贺
 * @version 2023/4/1 18:48
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Long id;
    private String  name;
    private Integer age;
}
