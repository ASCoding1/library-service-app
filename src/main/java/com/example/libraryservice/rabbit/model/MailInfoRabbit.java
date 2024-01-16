package com.example.libraryservice.rabbit.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MailInfoRabbit {
    private List<String> subscriberEmails = new ArrayList<>();
    private List<BookInfo> bookInfoList = new ArrayList<>();
}
