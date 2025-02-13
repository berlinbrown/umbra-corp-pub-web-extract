<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<%@ page import="java.util.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="com.umbra.social.services.BlogService" %>
<%@ page isErrorPage="true" %>
<html lang="en">
<head>
  <!-- Basic Page Needs
  –––––––––––––––––––––––––––––––––––––––––––––––––– -->
  <meta charset="utf-8">
  <title>Umbra Social - Blogs</title>
  <meta name="description" content="">
  <meta name="author" content="">

  <!-- Mobile Specific Metas
  –––––––––––––––––––––––––––––––––––––––––––––––––– -->
  <meta name="viewport" content="width=device-width, initial-scale=1">

  <!-- FONT
  –––––––––––––––––––––––––––––––––––––––––––––––––– -->
  <link href="//fonts.googleapis.com/css?family=Raleway:400,300,600" rel="stylesheet" type="text/css">
  <!-- CSS
  –––––––––––––––––––––––––––––––––––––––––––––––––– -->
  <link rel="stylesheet" href="umbramin/support/css/normalize.css">
  <link rel="stylesheet" href="umbramin/support/css/skeleton.css">
</head>
<body>

  <!-- Primary Page Layout
  –––––––––––––––––––––––––––––––––––––––––––––––––– -->
  <div class="container">
    <div class="row">
      <div class="one-half column" style="margin-top: 5%">
        <h4>Umbra Blog</h4>

        <img src="experimental/images/basic_scene.png" />
        <p>Main Blog Entries</p>

        <div>
          <%
          BlogService.blogdb();
          final String version = BlogService.version();
          final List<String> list = BlogService.data();
           %>
          version: <%= version %>
          <br />
          <%
            int i = 0;
            for (final String item  : list) {
          %>
          Blog Entry: <a href="<%= item %>"><%= item %></a>
          <br />
          <%
              i++;
              if (i > 14) {
                break;
              }
           }
          %>
        </div>
      </div>
    </div>
  </div>

<!-- End Document
  –––––––––––––––––––––––––––––––––––––––––––––––––– -->

</body>
</html>
