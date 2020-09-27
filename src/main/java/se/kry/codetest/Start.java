package se.kry.codetest;

import io.vertx.core.Vertx;
import se.kry.codetest.controller.MainVerticle;

public class Start {

  public static void main(String[] args) {
    Vertx.vertx().deployVerticle(new MainVerticle());
  }
}
