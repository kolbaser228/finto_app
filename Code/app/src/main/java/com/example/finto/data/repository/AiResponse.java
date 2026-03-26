package com.example.finto.data.repository;

import java.util.List;

public class AiResponse {
    public boolean is_receipt;
    public String error_message;
    public List<ParsedItem> transactions;
}