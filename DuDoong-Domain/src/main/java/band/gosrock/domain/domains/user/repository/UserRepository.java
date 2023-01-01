package band.gosrock.domain.domain.user.repository;


import band.gosrock.domain.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {}
