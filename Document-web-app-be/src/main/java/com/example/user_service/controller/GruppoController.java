    package com.example.user_service.controller;

    import java.util.Optional;

    import com.example.user_service.model.Gruppo;
    import com.example.user_service.model.User;
    import com.example.user_service.service.GruppoService;
    import com.example.user_service.service.UserService;

    import io.javalin.http.Context;
    import io.javalin.http.HttpStatus;
    import io.javalin.http.UnauthorizedResponse;

    public class GruppoController {

        private final GruppoService gruppoService;
        private final UserService userService;

        public GruppoController(GruppoService gruppoService, UserService userService) {
            this.gruppoService = gruppoService;
            this.userService = userService;
        }

        public void getAll(Context ctx) {
            ctx.json(gruppoService.getAllGruppi());
        }

        public void getOne(Context ctx) {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            Optional<Gruppo> gruppo = gruppoService.getGruppoById(id);
            if (gruppo.isPresent()) {
                ctx.json(gruppo.get());
            } else {
                ctx.status(HttpStatus.NOT_FOUND).result("Gruppo non trovato");
            }
        }

        public void create(Context ctx) {
            GruppoDto input = ctx.bodyAsClass(GruppoDto.class);

            Integer userId = ctx.attribute("userId");
            if (userId == null) {
                throw new UnauthorizedResponse("Utente non autenticato");
            }
            User owner = userService.getProfile(userId);
            Gruppo gruppo = gruppoService.createGruppo(input.name, owner);
            ctx.status(HttpStatus.CREATED).json(gruppo);
        }

        public void delete(Context ctx) {
            long id = ctx.pathParamAsClass("id", Long.class).get();
            gruppoService.deleteGruppo(id);
            ctx.status(HttpStatus.NO_CONTENT);
        }

        public static class GruppoDto {

            public String name;
        }
    }
