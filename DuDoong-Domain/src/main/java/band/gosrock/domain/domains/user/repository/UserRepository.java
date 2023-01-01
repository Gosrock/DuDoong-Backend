package band.gosrock.domain.domains.user.repository;


import band.gosrock.domain.domains.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {}
