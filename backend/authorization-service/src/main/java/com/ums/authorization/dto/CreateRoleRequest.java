package com.ums.authorization.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoleRequest {

	private String name;

	private String description;
}