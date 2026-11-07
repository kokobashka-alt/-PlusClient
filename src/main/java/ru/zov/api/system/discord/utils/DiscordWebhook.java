package ru.zov.api.system.discord.utils;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import javax.net.ssl.HttpsURLConnection;
import ru.nexusguard.protection.annotations.Native;

public class DiscordWebhook {
   private final String url;
   private String content;
   private String username;
   private String avatarUrl;
   private boolean tts;
   private final List<DiscordWebhook.EmbedObject> embeds = new ArrayList<>();

   public DiscordWebhook(String url) {
      this.url = url;
   }

   public void addEmbed(DiscordWebhook.EmbedObject embed) {
      this.embeds.add(embed);
   }

   @Native
   public void execute() throws IOException {
      if (this.content == null && this.embeds.isEmpty()) {
         throw new IllegalArgumentException("Set content or add at least one EmbedObject");
      }

      DiscordWebhook.JSONObject json = new DiscordWebhook.JSONObject();
      json.put("content", this.content);
      json.put("username", this.username);
      json.put("avatar_url", this.avatarUrl);
      json.put("tts", this.tts);
      if (!this.embeds.isEmpty()) {
         List<DiscordWebhook.JSONObject> embedObjects = new ArrayList<>();

         for (DiscordWebhook.EmbedObject embed : this.embeds) {
            DiscordWebhook.JSONObject jsonEmbed = new DiscordWebhook.JSONObject();
            jsonEmbed.put("title", embed.title);
            jsonEmbed.put("description", embed.description);
            jsonEmbed.put("url", embed.url);
            if (embed.color != null) {
               Color color = embed.color;
               int rgb = color.getRed();
               rgb = (rgb << 8) + color.getGreen();
               rgb = (rgb << 8) + color.getBlue();
               jsonEmbed.put("color", rgb);
            }

            DiscordWebhook.EmbedObject.Footer footer = embed.footer;
            DiscordWebhook.EmbedObject.Image image = embed.image;
            DiscordWebhook.EmbedObject.Thumbnail thumbnail = embed.thumbnail;
            DiscordWebhook.EmbedObject.Author author = embed.author;
            List<DiscordWebhook.EmbedObject.Field> fields = embed.fields;
            if (footer != null) {
               DiscordWebhook.JSONObject jsonFooter = new DiscordWebhook.JSONObject();
               jsonFooter.put("text", footer.getText());
               jsonFooter.put("icon_url", footer.getIconUrl());
               jsonEmbed.put("footer", jsonFooter);
            }

            if (image != null) {
               DiscordWebhook.JSONObject jsonImage = new DiscordWebhook.JSONObject();
               jsonImage.put("url", image.getUrl());
               jsonEmbed.put("image", jsonImage);
            }

            if (thumbnail != null) {
               DiscordWebhook.JSONObject jsonThumbnail = new DiscordWebhook.JSONObject();
               jsonThumbnail.put("url", thumbnail.getUrl());
               jsonEmbed.put("thumbnail", jsonThumbnail);
            }

            if (author != null) {
               DiscordWebhook.JSONObject jsonAuthor = new DiscordWebhook.JSONObject();
               jsonAuthor.put("name", author.getName());
               jsonAuthor.put("url", author.getUrl());
               jsonAuthor.put("icon_url", author.getIconUrl());
               jsonEmbed.put("author", jsonAuthor);
            }

            List<DiscordWebhook.JSONObject> jsonFields = new ArrayList<>();

            for (DiscordWebhook.EmbedObject.Field field : fields) {
               DiscordWebhook.JSONObject jsonField = new DiscordWebhook.JSONObject();
               jsonField.put("name", field.getName());
               jsonField.put("value", field.getValue());
               jsonField.put("inline", field.isInline());
               jsonFields.add(jsonField);
            }

            jsonEmbed.put("fields", jsonFields.toArray());
            embedObjects.add(jsonEmbed);
         }

         json.put("embeds", embedObjects.toArray());
      }

      HttpsURLConnection connection = (HttpsURLConnection)new URL(this.url).openConnection();
      connection.addRequestProperty("Content-Type", "application/json");
      connection.addRequestProperty("User-Agent", "Java-DiscordWebhook-BY-Gelox_");
      connection.setDoOutput(true);
      connection.setRequestMethod("POST");
      OutputStream stream = connection.getOutputStream();
      stream.write(json.toString().getBytes(StandardCharsets.UTF_8));
      stream.flush();
      stream.close();
      connection.getInputStream().close();
      connection.disconnect();
   }

   public static class EmbedObject {
      private String title;
      private String description;
      private String url;
      private Color color;
      private DiscordWebhook.EmbedObject.Footer footer;
      private DiscordWebhook.EmbedObject.Thumbnail thumbnail;
      private DiscordWebhook.EmbedObject.Image image;
      private DiscordWebhook.EmbedObject.Author author;
      private final List<DiscordWebhook.EmbedObject.Field> fields = new ArrayList<>();

      public DiscordWebhook.EmbedObject setTitle(String title) {
         this.title = title;
         return this;
      }

      public DiscordWebhook.EmbedObject setDescription(String description) {
         this.description = description;
         return this;
      }

      public DiscordWebhook.EmbedObject setUrl(String url) {
         this.url = url;
         return this;
      }

      public DiscordWebhook.EmbedObject setColor(Color color) {
         this.color = color;
         return this;
      }

      public DiscordWebhook.EmbedObject setFooter(String text, String icon) {
         this.footer = new DiscordWebhook.EmbedObject.Footer(text, icon);
         return this;
      }

      public DiscordWebhook.EmbedObject setThumbnail(String url) {
         this.thumbnail = new DiscordWebhook.EmbedObject.Thumbnail(url);
         return this;
      }

      public DiscordWebhook.EmbedObject setImage(String url) {
         this.image = new DiscordWebhook.EmbedObject.Image(url);
         return this;
      }

      public DiscordWebhook.EmbedObject setAuthor(String name, String url, String icon) {
         this.author = new DiscordWebhook.EmbedObject.Author(name, url, icon);
         return this;
      }

      public DiscordWebhook.EmbedObject addField(String name, String value, boolean inline) {
         this.fields.add(new DiscordWebhook.EmbedObject.Field(name, value, inline));
         return this;
      }

      private class Author {
         private final String name;
         private final String url;
         private final String iconUrl;

         private Author(String name, String url, String iconUrl) {
            this.name = name;
            this.url = url;
            this.iconUrl = iconUrl;
         }

         private String getName() {
            return this.name;
         }

         private String getUrl() {
            return this.url;
         }

         private String getIconUrl() {
            return this.iconUrl;
         }
      }

      private class Field {
         private final String name;
         private final String value;
         private final boolean inline;

         private Field(String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
         }

         private String getName() {
            return this.name;
         }

         private String getValue() {
            return this.value;
         }

         private boolean isInline() {
            return this.inline;
         }
      }

      private class Footer {
         private final String text;
         private final String iconUrl;

         private Footer(String text, String iconUrl) {
            this.text = text;
            this.iconUrl = iconUrl;
         }

         private String getText() {
            return this.text;
         }

         private String getIconUrl() {
            return this.iconUrl;
         }
      }

      private class Image {
         private String url;

         private Image(String url) {
            this.url = url;
         }

         private String getUrl() {
            return this.url;
         }
      }

      private class Thumbnail {
         private final String url;

         private Thumbnail(String url) {
            this.url = url;
         }

         private String getUrl() {
            return this.url;
         }
      }
   }

   private class JSONObject {
      private final HashMap<String, Object> map = new HashMap<>();

      void put(String key, Object value) {
         if (value != null) {
            this.map.put(key, value);
         }
      }

      @Override
      public String toString() {
         StringBuilder builder = new StringBuilder();
         Set<Entry<String, Object>> entrySet = this.map.entrySet();
         builder.append("{");
         int i = 0;

         for (Entry<String, Object> entry : entrySet) {
            Object val = entry.getValue();
            builder.append(this.quote(entry.getKey())).append(":");
            if (val instanceof String) {
               builder.append(this.quote(String.valueOf(val)));
            } else if (val instanceof Integer) {
               builder.append(Integer.valueOf(String.valueOf(val)));
            } else if (val instanceof Boolean) {
               builder.append(val);
            } else if (val instanceof DiscordWebhook.JSONObject) {
               builder.append(val);
            } else if (val.getClass().isArray()) {
               builder.append("[");
               int len = Array.getLength(val);

               for (int j = 0; j < len; j++) {
                  builder.append(Array.get(val, j).toString()).append(j != len - 1 ? "," : "");
               }

               builder.append("]");
            }

            i++;
            builder.append(i == entrySet.size() ? "}" : ",");
         }

         return builder.toString();
      }

      private String quote(String string) {
         return "\"" + string + "\"";
      }
   }
}
