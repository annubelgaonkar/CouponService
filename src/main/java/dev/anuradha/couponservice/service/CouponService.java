package dev.anuradha.couponservice.service;

import dev.anuradha.couponservice.model.Coupon;
import dev.anuradha.couponservice.repositories.CouponRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository repo;

    public Coupon create(Coupon coupon){
        return repo.save(coupon);
    }

    public List<Coupon> couponList(){
        return repo.findAll();
    }

    public Optional<Coupon> findById(String id){
        return repo.findById(id);
    }

    @Transactional
    public Optional<Coupon> update(String id, Coupon updated){
        return repo.findById(id).map(existing ->{
            existing.setCode(updated.getCode());
            existing.setType(updated.getType());
            existing.setDetails(updated.getDetails());
            existing.setActive(updated.isActive());
            return existing;
        });
    }

    public void delete(String id){
        repo.deleteById(id);
    }

    public Optional<Coupon> findByCode(String code){
        return repo.findByCode(code);
    }
}
