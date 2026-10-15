package System_Security.tp_jwt_spring.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.AnyDiscriminatorImplicitValues;
@NoArgsConstructor
@Data @AllArgsConstructor @Entity

@Table(name = "user")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;
    @Column(unique=true)
    private String email;
    private String password;
    private String role;
 }
