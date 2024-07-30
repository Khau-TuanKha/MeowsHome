package com.asm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.asm.bean.Account;
import com.asm.bean.Role;
import com.asm.bean.RoleDetail;
import com.asm.service.AccountService;
import com.asm.service.BrandService;
import com.asm.service.CategoryService;
import com.asm.service.MailerService;
import com.asm.service.ProductService;
import com.asm.service.SessionService;

@Controller
public class HomeController {
    @Autowired
    BrandService bService;
    @Autowired
    CategoryService cService;
    @Autowired
    ProductService pService;
    @Autowired
    SessionService session;
    @Autowired
    AccountService aService;
    @Autowired
    MailerService mailer;

    @RequestMapping("/admin")
    public String admin() {
        return "admin/index";
    }

    @RequestMapping("/")
    public String home(Model model) {
        // load ds product xep theo ngay tao
        model.addAttribute("db", pService.findProductByCreateDateDESC());
        return "home/index";
    }

    @GetMapping("/brand/list")
    public String brandList(Model model) {
        return "brand/list";
    }

    @GetMapping("/register")
    public String register(@ModelAttribute Account account) {
        return "register";
    }

    @PostMapping("/register")
    public String signup(Model model,
                         @ModelAttribute Account account) {
        if (aService.existsById(account.getUsername())) {
            model.addAttribute("error", "Đã tồn tại username " + account.getUsername());
            return "register";
        } else {
            account.setActivated(true);

            account.setPhoto("logo.jpg");

            Role r = new Role();
            r.setRole("user");
            RoleDetail rd = new RoleDetail();
            rd.setAccount(account);
            rd.setRole(r);

            aService.save(account);
            aService.saveRoleDetail(rd);
            return "redirect:/register/success";
        }
    }

    @RequestMapping("/register/success")
    public String registerSuccess(Model model) {
        model.addAttribute("message", "Đăng ký thành công");
        return "login";
    }

    @GetMapping("/login")
    public String formLogin(Model model, @RequestParam(value = "message", required = false) String message) {
        model.addAttribute("message", message);
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        Model model) {
        try {
            Account account = aService.findByUsername(username);
            if (!account.getPassword().equals(password)) {
                model.addAttribute("message", "Invalid password");
            } else {
                String uri = session.get("security-uri");
//				if(uri != null) {
//					return "redirect:"+uri;
//				}
//				else {
                session.set("user", account);
                if (this.checkAdmin(account)) {
                    session.set("userAdmin", "admin");
                }
                model.addAttribute("message", "Login success");
//				}
            }
        } catch (Exception e) {
            // TODO: handle exception
            model.addAttribute("message", "Invalid username");
        }
        return "login";
    }

    public Boolean checkAdmin(Account account) {
        for (RoleDetail roleDetail : account.getRoleDetails()) {
            if (roleDetail.getRole().getRole().equals("staff") || roleDetail.getRole().getRole().equals("director")) {
                return true;
            }
        }
        return false;
    }

    @RequestMapping("/logout")
    public String logoutSuccess(Model model) {
        session.remove("user");
        session.remove("userAdmin");
        session.remove("security-uri");
        session.remove("uri");
        model.addAttribute("message", "Đăng xuất thành công");
        return "login";
    }

    @GetMapping("forgot-password")
    public String forgot() {
        return "forgot";
    }

    @PostMapping("forgot-password")
    public String forgot(@RequestParam("username") String username, Model model) {
        try {
            Account account = aService.findByUsername(username);
            String to = account.getEmail();
            String email = to.substring(0, 2);

            double randomDouble = Math.random();
            randomDouble = randomDouble * 1000000 + 1;
            int randomInt = (int) randomDouble;

            String subject = "Lấy lại mật khẩu";
            String body = "<html>"
                    + "<head>"
                    + "<style>"
                    + "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; text-align: center; }"
                    + ".container { width: 80%; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px; background-color: #f9f9f9; display: inline-block; text-align: center; }"
                    + "h2 { color: #2c3e50; }"
                    + "p { margin: 10px 0; }"
                    + ".password-box { display: inline-block; padding: 10px 20px; font-size: 18px; font-weight: bold; color: blue; background-color: white; border-radius: 5px; text-decoration: none; }"
                    + ".footer { margin-top: 20px; font-size: 12px; color: #777; }"
                    + "</style>"
                    + "</head>"
                    + "<body>"
                    + "<div class='container'>"
                    + "<h2>Chào bạn,</h2>"
                    + "<p>Chúng tôi đã nhận được yêu cầu lấy lại mật khẩu của bạn.</p>"
                    + "<p>Mật khẩu mới của bạn là:</p>"
                    + "<p><a href='#' class='password-box'>" + randomInt + "</a></p>"
                    + "<p>Hãy đăng nhập và thay đổi mật khẩu ngay để đảm bảo an toàn.</p>"
                    + "<div class='footer'>"
                    + "<p>Trân trọng,</p>"
                    + "<p>Đội ngũ hỗ trợ</p>"
                    + "</div>"
                    + "</div>"
                    + "</body>"
                    + "</html>";
            mailer.send(to, subject, body);

            account.setPassword(String.valueOf(randomInt));
            aService.save(account);

            model.addAttribute("message", "Mật khẩu mới đã được gửi đến mail " + email + "***");
        } catch (Exception e) {
            // TODO: handle exception
            model.addAttribute("message", "Invalid Username");
        }
        return "forgot";
    }
}
