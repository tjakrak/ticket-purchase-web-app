package cs601.project4.controller;

import com.google.gson.Gson;
import cs601.project4.database.DBCPDataSource;
import cs601.project4.database.DataFetcherManager;
import cs601.project4.database.DataUpdaterManager;
import cs601.project4.utilities.LoginConstants;
import cs601.project4.tableobject.ClientInfo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.SQLException;

@Controller
public class SettingController {
    Gson gson = new Gson();

    @GetMapping("/user/settings")
    public String getUserSetting(Model model, HttpServletRequest req) {
        ClientInfo clientInfo = getClientInfo(req);

        if (clientInfo == null) { // if the user hasn't logged in to the app
            req.getSession().setAttribute(LoginConstants.IS_FAIL_TO_LOGIN, "1");
            return "redirect:/login ";
        }

        String userId = clientInfo.getUniqueId();

        try (Connection connection = DBCPDataSource.getConnection()){
            clientInfo = DataFetcherManager.getClientInfo(connection, userId, "");
        } catch(SQLException e) {
            e.printStackTrace();
        }

        model.addAttribute("currName", clientInfo.getName());
        model.addAttribute("currEmail", clientInfo.getEmail());
        model.addAttribute("currZipcode", clientInfo.getZipcode());
        model.addAttribute("clientInfo", new ClientInfo());
        return "setting";
    }

    @PostMapping("/user/settings")
    public String postUserSetting(@ModelAttribute("settingBean") ClientInfo settingBean, HttpServletRequest req) {

        ClientInfo clientInfo = getClientInfo(req);

        if (clientInfo == null) { // if the user hasn't logged in to the app
            req.getSession().setAttribute(LoginConstants.IS_FAIL_TO_LOGIN, "1");
            return "redirect:/login ";
        }

        String name = settingBean.getName();
        String email = settingBean.getEmail();
        String zipcode = settingBean.getZipcode();

        String validationMsg = validateInput(name, email);

        if (validationMsg.equals("validated")) {
            String userId = clientInfo.getUniqueId();
            updateUserInfoDatabase(name, email, zipcode, userId);
        }

        // update the userinfo in the session attribute
        req.getSession().removeAttribute(LoginConstants.CLIENT_INFO_KEY);
        req.getSession().setAttribute(LoginConstants.CLIENT_INFO_KEY,  new Gson().toJson(clientInfo));

        return "redirect:/user/settings";
    }

    private String validateInput(String name, String email) {
        if (email.equals("") || name.equals("")) {
            return "Name or email cannot be empty";
        } else if (!email.contains("@") || !email.contains(".")) {
            return "Please put a valid email address";
        } else {
            return "validated";
        }
    }

    // move this to other method
    private ClientInfo getClientInfo(HttpServletRequest req) {
        return gson.fromJson((String) req.getSession().getAttribute(LoginConstants.CLIENT_INFO_KEY), ClientInfo.class);
    }

    private void  updateUserInfoDatabase(String name, String email, String zipcode, String userId) {
        try (Connection connection = DBCPDataSource.getConnection()){
            DataUpdaterManager.updateUser(connection, name, email, zipcode, userId);
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

}
