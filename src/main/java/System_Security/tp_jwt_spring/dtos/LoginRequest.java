package System_Security.tp_jwt_spring.dtos;

public record LoginRequest(
        String email,
        String password
) {

}
