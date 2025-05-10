package klu.com.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import klu.com.model.Users;
import klu.com.model.UsersManager;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/users")
public class UsersController {
	
	@Autowired
	UsersManager UM;
	
	@PostMapping("/signup")
	public String insertUser(@RequestBody Users U) {
		return UM.addUser(U);
	}
	
	@PostMapping("/signin")
	public String signIn(@RequestBody Users U) {
		return UM.login(U.getEmail(),U.getPassword());
	}
	
	@PostMapping("/getfullname")
	public String getFullname(@RequestBody Map<String, String> data) {
		return UM.getFullname(data.get("csrid"));
	}
	
	@PostMapping("/getuserid")
	public String getUserId(@RequestBody Map<String, String> data) {
		return UM.getUserId(data.get("csrid"));
	}
	
	@GetMapping("/all")
	public List<Map<String, Object>> getAllUsers() {
		return UM.getAllUsers();
	}

}
