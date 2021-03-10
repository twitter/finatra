package com.twitter.finatra.http.streaming;

public class Lunch {
  public String drink;
  public long protein;
  public int carbs;

  public Lunch(String drink, long protein, int carbs) {
    this.drink = drink;
    this.protein = protein;
    this.carbs = carbs;
  }
}
