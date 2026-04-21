package com.seeker.test2.controller;

import com.seeker.test2.entity.WorkRecord;
import com.seeker.test2.repository.WorkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/backend")
public class BackendController {

    @Autowired
    private WorkRepository workRepository;

    @GetMapping("/work")
    public String doWork(@RequestParam(defaultValue = "SeekerTest") String name) {
        WorkRecord record = new WorkRecord(name, "Processed by Backend Service");
        workRepository.save(record);
        return "Work '" + name + "' completed and saved to DB.";
    }
}
