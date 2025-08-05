package com.github.dimitryivaniuta.videometadata.repository;

import com.github.dimitryivaniuta.videometadata.model.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, Long> {

    Mono<User> findByUsername(String username);

/*    @Query("""
           SELECT COUNT(*) 
           FROM users u
           WHERE (:q IS NULL OR :q = '' 
                  OR u.username ILIKE '%'||:q||'%' 
                  OR u.email    ILIKE '%'||:q||'%')
           """)
    Mono<Long> countBySearch(String q);*/
Mono<Long> count();
    Mono<Long> countByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String userName, String email);


    /* USERNAME */
    @Query("""
           SELECT u.* FROM users u
           WHERE (:q IS NULL OR :q = '' 
                  OR u.username ILIKE '%'||:q||'%')
           ORDER BY u.username ASC
           LIMIT :limit OFFSET :offset
           """)
    Flux<User> pageBySearchOrderByUsernameAsc(String q, long limit, long offset);

    @Query("""
           SELECT u.* FROM users u
           WHERE (:q IS NULL OR :q = '' 
                  OR u.username ILIKE '%'||:q||'%')
           ORDER BY u.username DESC
           LIMIT :limit OFFSET :offset
           """)
    Flux<User> pageBySearchOrderByUsernameDesc(String q, long limit, long offset);


    /* EMAIL */
    @Query("""
           SELECT u.* FROM users u
           WHERE (:q IS NULL OR :q = ''  
                  OR u.email    ILIKE '%'||:q||'%')
           ORDER BY u.username ASC
           LIMIT :limit OFFSET :offset
           """)
    Flux<User> pageBySearchOrderByEmailAsc(String q, long limit, long offset);

    @Query("""
           SELECT u.* FROM users u
           WHERE (:q IS NULL OR :q = ''  
                  OR u.email    ILIKE '%'||:q||'%')
           ORDER BY u.username DESC
           LIMIT :limit OFFSET :offset
           """)
    Flux<User> pageBySearchOrderByEmailDesc(String q, long limit, long offset);

    /* CREATED_AT */
    @Query("""
           SELECT u.* FROM users u
           WHERE (:q IS NULL OR :q = '' 
                  OR u.username ILIKE '%'||:q||'%' 
                  OR u.email    ILIKE '%'||:q||'%')
           ORDER BY u.created_at ASC
           LIMIT :limit OFFSET :offset
           """)
    Flux<User> pageBySearchOrderByCreatedAtAsc(String q, long limit, long offset);

    @Query("""
           SELECT u.* FROM users u
           WHERE (:q IS NULL OR :q = '' 
                  OR u.username ILIKE '%'||:q||'%' 
                  OR u.email    ILIKE '%'||:q||'%')
           ORDER BY u.created_at DESC
           LIMIT :limit OFFSET :offset
           """)
    Flux<User> pageBySearchOrderByCreatedAtDesc(String q, long limit, long offset);

    /* UPDATED_AT */
    @Query("""
           SELECT u.* FROM users u
           WHERE (:q IS NULL OR :q = '' 
                  OR u.username ILIKE '%'||:q||'%' 
                  OR u.email    ILIKE '%'||:q||'%')
           ORDER BY u.updated_at ASC
           LIMIT :limit OFFSET :offset
           """)
    Flux<User> pageBySearchOrderByUpdatedAtAsc(String q, long limit, long offset);

    @Query("""
           SELECT u.* FROM users u
           WHERE (:q IS NULL OR :q = '' 
                  OR u.username ILIKE '%'||:q||'%' 
                  OR u.email    ILIKE '%'||:q||'%')
           ORDER BY u.updated_at DESC
           LIMIT :limit OFFSET :offset
           """)
    Flux<User> pageBySearchOrderByUpdatedAtDesc(String q, long limit, long offset);

    /* LAST_LOGIN_AT */
    @Query("""
           SELECT u.* FROM users u
           WHERE (:q IS NULL OR :q = '' 
                  OR u.username ILIKE '%'||:q||'%' 
                  OR u.email    ILIKE '%'||:q||'%')
           ORDER BY u.last_login_at ASC NULLS FIRST
           LIMIT :limit OFFSET :offset
           """)
    Flux<User> pageBySearchOrderByLastLoginAtAsc(String q, long limit, long offset);

    @Query("""
           SELECT u.* FROM users u
           WHERE (:q IS NULL OR :q = '' 
                  OR u.username ILIKE '%'||:q||'%' 
                  OR u.email    ILIKE '%'||:q||'%')
           ORDER BY u.last_login_at DESC NULLS LAST
           LIMIT :limit OFFSET :offset
           """)
    Flux<User> pageBySearchOrderByLastLoginAtDesc(String q, long limit, long offset);
}