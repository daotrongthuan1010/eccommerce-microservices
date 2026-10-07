package com.dtthuan3.ecommerce.catalogservice.service;

import com.dtthuan3.ecommerce.catalogservice.dto.request.ManufacturersRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.ManufacturersRespone;

import java.util.List;

public interface ManufacturersService {

    ManufacturersRespone create(
            ManufacturersRequest request
    );

    ManufacturersRespone getById(
            Long id
    );

    List<ManufacturersRespone> getAll();

    ManufacturersRespone update(
            Long id,
            ManufacturersRequest request
    );

    void delete(Long id);
}