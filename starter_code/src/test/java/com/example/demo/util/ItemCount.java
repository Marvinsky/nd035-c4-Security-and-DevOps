package com.example.demo.util;

import com.example.demo.model.persistence.Item;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ItemCount {

  Item item;
  Integer count;

}
