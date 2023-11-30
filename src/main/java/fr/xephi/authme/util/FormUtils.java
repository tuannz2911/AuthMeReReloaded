package fr.xephi.authme.util;

import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public class FormUtils {
    private static final FloodgateApi floodgateApi = FloodgateApi.getInstance();
    private static final AuthMeApi authMeApi = AuthMeApi.getInstance();

    public static void openRegisterForm(Player player, String title, String text, String textBar) {
        String name = player.getName();
        UUID uuid = player.getUniqueId();
        CustomForm.Builder regForm = CustomForm.builder()
            .title(title)
            .input(text, textBar)
            .validResultHandler(((customForm1, customFormResponse) -> {
                authMeApi.registerPlayer(name, customFormResponse.asInput(0));
            }));
        floodgateApi.sendForm(uuid, regForm);
    }

}
