package cs601.project4.controller.home;

import com.google.gson.Gson;
import cs601.project4.server.LoginServerConstants;
import cs601.project4.server.NoStayHomeAppServer;
import cs601.project4.utilities.HTTPFetcher;
import cs601.project4.utilities.LoginUtilities;
import cs601.project4.utilities.gson.ClientInfo;
import cs601.project4.utilities.gson.SlackConfigApi;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.util.Map;

@Controller
public class LoginController {
    @GetMapping("/login")
    public String getLogin(Model model, HttpServletRequest req) throws FileNotFoundException {
        // retrieve the ID of this session
        String sessionId = req.getSession(true).getId();
        System.out.println(sessionId);

        // determine whether the user is already authenticated
        Object clientInfoObj = req.getSession().getAttribute(LoginServerConstants.CLIENT_INFO_KEY);
        Object clientConfigKeyObj = req.getSession().getAttribute(LoginServerConstants.CONFIG_KEY);
        if (clientInfoObj != null) { // if the user already authenticated, then go to homepage
            return "redirect:/home ";
        } else if(clientConfigKeyObj != null) { // authenticated the user after pressing the slack login button
            Boolean verified = slackLoginVerifier(model, req, sessionId);

            if(!verified) {
                return "redirect:/login";
            } else {
                return "redirect:/home";
            }
        } else {
            // to be passed to slackAPI for oauth
            String state = sessionId;
            String nonce = LoginUtilities.generateNonce(state);

            // adding client_key, secret_key and redirect uri information to the session attribute (so it can be shared across different controller)
            // store it as json formatted string (GSON -> json)
            SlackConfigApi config = NoStayHomeAppServer.getConfig();
            req.getSession().setAttribute(LoginServerConstants.CONFIG_KEY, new Gson().toJson(config));

            // Generate url to send a request to SlackApi
            String url = LoginUtilities.generateSlackAuthorizeURL(config.getClient_id(),
                    state,
                    nonce,
                    config.getRedirect_url());

            // adding the url to model so it can be read by thymeleaf html
            model.addAttribute("url", url);

            return "login";
        }
    }

    /**
     * A helper wrapper method to help verify the login through slack
     * @param model
     * @param req necessary to get session id, attribute, all information from slack response
     * @param sessionId current session id
     */
    private Boolean slackLoginVerifier(Model model, HttpServletRequest req, String sessionId) {
        // retrieve the config info from the session attribute and convert it to Config object
        Gson gson = new Gson();
        SlackConfigApi slackConfigApi = gson.fromJson((String) req.getSession().getAttribute(LoginServerConstants.CONFIG_KEY), SlackConfigApi.class);

        // getting the "code" parameter from the SLACK response
        String code = req.getParameter(LoginServerConstants.CODE_KEY);

        // generate string url for slack from the config: client_id, client_secret, redirect_url
        String url = LoginUtilities.generateSlackTokenURL(slackConfigApi.getClient_id(), slackConfigApi.getClient_secret(), code, slackConfigApi.getRedirect_url());

        // send get response to slack with the url generated above
        String responseString = HTTPFetcher.doGet(url, null);

        // get the slack json response and put it to hashmap as key and value
        Map<String, Object> response = LoginUtilities.jsonStrToMap(responseString);

        // verifying the response from slack API
        ClientInfo clientInfo = LoginUtilities.verifyTokenResponse(response, sessionId);

        // if the user is not verified
        if(clientInfo == null) {
            return false;
            // if the user is verified
        } else {
            req.getSession().setAttribute(LoginServerConstants.CLIENT_INFO_KEY, new Gson().toJson(clientInfo));
            return true;
        }
    }
}