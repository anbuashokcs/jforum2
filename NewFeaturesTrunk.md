# New and changed features that have not yet been released #

---

JForum is in constant development and improvement, and we always try to ship the best code possible. The minimum required Java version is now Java 6.


## Libraries ##

---

  * upgrade PostgreSQL driver from 9.3-1102-jdbc4 to 9.3-1103-jdbc4
  * upgrade FreeMarker from 2.3.21 to 2.3.22
  * upgrade JDOM2 from 2.0.5 to 2.0.6


## New Features and fixes ##

---

  * Better fix for "Arbitrary File Upload and Remote Code Execution – Smileys"
  * Only show number of edits of a post to logged-in users
  * Don't ignore user's "Hide my online status" setting
  * Salt the user password with an installation-specific value
  * Fix problem where smilies could not be inserted into posts by clicking their icons
  * Added some missing settings to the Configurations page
  * Dates can be displayed in the local timezone of the forum visitor instead of the timezone of the forum server
  * [broken "can edit message" setting](http://jforum.andowson.com/posts/list/117.page) has been fixed
  * Numerous code improvements suggested by FindBugs

## New Configurations ##

---

|**Entry name**|**Default value**|**Description**|
|:-------------|:----------------|:--------------|
|dateTime.local|false|whether to display dates in the visitor timezone or the server timezone|


## Database Schema ##

---