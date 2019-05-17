package com.twitter.web.dashboard.controllers

import com.twitter.finagle.http.MediaType
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.web.dashboard.views.UserView

class DashboardController extends Controller {

  get("/ping") { request: Request =>
    response.ok.file("ping.txt")
  }

  get("/other") { request: Request =>
    response.ok.file("/subdirectory1/other.html")
  }

  get("/subdirectory2") { request: Request =>
    response.ok.file("/subdirectory2/index.html")
  }

  get("/user") { request: Request =>
    val firstName = request.params("first")
    val lastName = request.params("last")
    UserView(firstName, lastName)
  }

  get("/document.xml") { request: Request =>
    response.ok
      .contentType(MediaType.XmlUtf8)
      .body("""<?xml version="1.0" encoding="UTF-8" ?>
          |<records>
          |	<record>
          |		<foo>Ac Eleifend Vitae Industries</foo>
          |		<bar>Colette P. Phelps</bar>
          |		<baz>Kiayada</baz>
          |		<bat>1-656-455-8396</bat>
          |		<field>ac, eleifend vitae, erat. Vivamus nisi. Mauris nulla. Integer urna.</field>
          |		<something>A392D536-0E33-030B-96CD-7D57B939E0A0</something>
          |	</record>
          |	<record>
          |		<foo>Arcu Limited</foo>
          |		<bar>Ali Y. Mcclure</bar>
          |		<baz>Yeo</baz>
          |		<bat>717-7244</bat>
          |		<field>Sed neque. Sed eget lacus. Mauris non dui nec urna</field>
          |		<something>0312A677-EB7E-6C61-02B3-6918B91C5CE8</something>
          |	</record>
          |	<record>
          |		<foo>Arcu Vel Quam Limited</foo>
          |		<bar>Vivian T. Simon</bar>
          |		<baz>Tamekah</baz>
          |		<bat>1-345-221-8101</bat>
          |		<field>magna. Cras convallis convallis dolor. Quisque tincidunt pede ac urna.</field>
          |		<something>5449182E-EBFA-7299-BD9B-A97C3F37D1C6</something>
          |	</record>
          |	<record>
          |		<foo>Tincidunt Aliquam Arcu Consulting</foo>
          |		<bar>Dustin X. Riddle</bar>
          |		<baz>Lavinia</baz>
          |		<bat>1-418-188-4243</bat>
          |		<field>turpis nec mauris blandit mattis. Cras eget nisi dictum augue</field>
          |		<something>5497DE58-57E3-4BE2-F095-AE2D3CE38C57</something>
          |	</record>
          |	<record>
          |		<foo>Curabitur Dictum Institute</foo>
          |		<bar>Cheyenne S. Spears</bar>
          |		<baz>Yoshio</baz>
          |		<bat>1-187-543-9765</bat>
          |		<field>Quisque ornare tortor at risus. Nunc ac sem ut dolor</field>
          |		<something>8BBD366A-55E3-56C5-6994-8F40674701B1</something>
          |	</record>
          |	<record>
          |		<foo>Ligula Consectetuer Incorporated</foo>
          |		<bar>Rosalyn H. Benton</bar>
          |		<baz>Kadeem</baz>
          |		<bat>1-167-764-0345</bat>
          |		<field>Nulla eu neque pellentesque massa lobortis ultrices. Vivamus rhoncus. Donec</field>
          |		<something>F9B05C15-93D5-9F4F-D6CC-EB7DBCD4F510</something>
          |	</record>
          |	<record>
          |		<foo>Nec Tempus Associates</foo>
          |		<bar>Bruce Z. Brown</bar>
          |		<baz>Kristen</baz>
          |		<bat>814-7495</bat>
          |		<field>neque tellus, imperdiet non, vestibulum nec, euismod in, dolor. Fusce</field>
          |		<something>47C93A3D-9653-0713-5FC7-16EF484071C5</something>
          |	</record>
          |	<record>
          |		<foo>Feugiat Ltd</foo>
          |		<bar>Martena W. Mcdonald</bar>
          |		<baz>Fulton</baz>
          |		<bat>567-7322</bat>
          |		<field>turpis. Aliquam adipiscing lobortis risus. In mi pede, nonummy ut,</field>
          |		<something>BD10E667-EE8A-5070-5DE6-B879E886165A</something>
          |	</record>
          |	<record>
          |		<foo>Rutrum Fusce Incorporated</foo>
          |		<bar>Risa G. Nolan</bar>
          |		<baz>Lev</baz>
          |		<bat>1-865-502-9933</bat>
          |		<field>enim. Mauris quis turpis vitae purus gravida sagittis. Duis gravida.</field>
          |		<something>9B83B1C8-519C-4985-6465-0341D033D34D</something>
          |	</record>
          |	<record>
          |		<foo>Nulla Interdum Curabitur Industries</foo>
          |		<bar>Kay W. Townsend</bar>
          |		<baz>Raphael</baz>
          |		<bat>480-3801</bat>
          |		<field>hendrerit a, arcu. Sed et libero. Proin mi. Aliquam gravida</field>
          |		<something>F6BBF41C-A3DF-04E5-C66C-285A13D45F4B</something>
          |	</record>
          |	<record>
          |		<foo>Nam Consequat Corporation</foo>
          |		<bar>Ella F. Roth</bar>
          |		<baz>Talon</baz>
          |		<bat>1-887-526-8349</bat>
          |		<field>est arcu ac orci. Ut semper pretium neque. Morbi quis</field>
          |		<something>15196944-B953-414C-9E1E-7E58A6944AA3</something>
          |	</record>
          |	<record>
          |		<foo>Phasellus Ornare Fusce Consulting</foo>
          |		<bar>Alexander C. Farmer</bar>
          |		<baz>Ross</baz>
          |		<bat>1-121-920-8757</bat>
          |		<field>Pellentesque habitant morbi tristique senectus et netus et malesuada fames</field>
          |		<something>A7E0EE93-3094-4099-EF13-A5028D713D64</something>
          |	</record>
          |	<record>
          |		<foo>Phasellus Industries</foo>
          |		<bar>Darius A. Sparks</bar>
          |		<baz>Graham</baz>
          |		<bat>599-8003</bat>
          |		<field>leo. Vivamus nibh dolor, nonummy ac, feugiat non, lobortis quis,</field>
          |		<something>CE6A73B5-7BB6-BA2B-AF59-F624AF341F4B</something>
          |	</record>
          |	<record>
          |		<foo>Orci Lobortis Corporation</foo>
          |		<bar>Travis H. Collier</bar>
          |		<baz>Keiko</baz>
          |		<bat>1-889-717-8913</bat>
          |		<field>aliquet diam. Sed diam lorem, auctor quis, tristique ac, eleifend</field>
          |		<something>F7B3BEBE-D766-7E35-0243-7DA6038E3C8C</something>
          |	</record>
          |	<record>
          |		<foo>Diam Foundation</foo>
          |		<bar>Yeo M. Hughes</bar>
          |		<baz>Brendan</baz>
          |		<bat>733-2649</bat>
          |		<field>ipsum primis in faucibus orci luctus et ultrices posuere cubilia</field>
          |		<something>7D6A624B-AEAB-9EAA-A114-B425F1DB5BB9</something>
          |	</record>
          |	<record>
          |		<foo>Consequat Auctor LLC</foo>
          |		<bar>Yoshi U. Mccall</bar>
          |		<baz>Phyllis</baz>
          |		<bat>806-2753</bat>
          |		<field>cursus et, eros. Proin ultrices. Duis volutpat nunc sit amet</field>
          |		<something>5379ADAE-878A-F863-6056-08C37DE183F8</something>
          |	</record>
          |	<record>
          |		<foo>Commodo Hendrerit Donec Associates</foo>
          |		<bar>Ferdinand A. Horne</bar>
          |		<baz>Savannah</baz>
          |		<bat>1-436-770-8261</bat>
          |		<field>blandit congue. In scelerisque scelerisque dui. Suspendisse ac metus vitae</field>
          |		<something>EC786926-5CCD-A853-07D2-508947D9F599</something>
          |	</record>
          |	<record>
          |		<foo>Metus In Consulting</foo>
          |		<bar>Orli V. Mendoza</bar>
          |		<baz>Emmanuel</baz>
          |		<bat>431-7053</bat>
          |		<field>pede. Cras vulputate velit eu sem. Pellentesque ut ipsum ac</field>
          |		<something>26E4B471-7A38-37A5-31D8-FD682DDF589C</something>
          |	</record>
          |	<record>
          |		<foo>Lorem Eget Institute</foo>
          |		<bar>Heather K. Mccall</bar>
          |		<baz>Edward</baz>
          |		<bat>660-8748</bat>
          |		<field>Nulla dignissim. Maecenas ornare egestas ligula. Nullam feugiat placerat velit.</field>
          |		<something>D65FF948-B572-0611-B6F3-D61B671A0EE1</something>
          |	</record>
          |	<record>
          |		<foo>Tellus LLC</foo>
          |		<bar>Lucian O. Weiss</bar>
          |		<baz>Marcia</baz>
          |		<bat>680-6400</bat>
          |		<field>Aliquam erat volutpat. Nulla dignissim. Maecenas ornare egestas ligula. Nullam</field>
          |		<something>F92BCDC8-1A75-4C23-3497-A9C984EFA0E8</something>
          |	</record>
          |	<record>
          |		<foo>Massa Foundation</foo>
          |		<bar>Aileen N. Flowers</bar>
          |		<baz>Quincy</baz>
          |		<bat>653-1510</bat>
          |		<field>diam nunc, ullamcorper eu, euismod ac, fermentum vel, mauris. Integer</field>
          |		<something>FFC64A85-3871-E911-8545-9429E11876D3</something>
          |	</record>
          |	<record>
          |		<foo>Molestie Limited</foo>
          |		<bar>Felix U. Franco</bar>
          |		<baz>Merrill</baz>
          |		<bat>548-8159</bat>
          |		<field>lacus. Cras interdum. Nunc sollicitudin commodo ipsum. Suspendisse non leo.</field>
          |		<something>EC0DC800-A208-CFB1-921B-C4CA10B7EBAC</something>
          |	</record>
          |	<record>
          |		<foo>Et Company</foo>
          |		<bar>Lenore K. Frazier</bar>
          |		<baz>Emerson</baz>
          |		<bat>494-2769</bat>
          |		<field>erat, eget tincidunt dui augue eu tellus. Phasellus elit pede,</field>
          |		<something>EF4CE0D3-302B-C646-DBD8-7DD5DF7EB34A</something>
          |	</record>
          |	<record>
          |		<foo>Semper LLP</foo>
          |		<bar>Grady V. Riggs</bar>
          |		<baz>Robin</baz>
          |		<bat>1-988-805-4446</bat>
          |		<field>massa. Vestibulum accumsan neque et nunc. Quisque ornare tortor at</field>
          |		<something>4C2DA073-630F-3AEF-E52F-DA3E65E974C9</something>
          |	</record>
          |	<record>
          |		<foo>Sed Hendrerit A Corporation</foo>
          |		<bar>Calvin U. Daniel</bar>
          |		<baz>Keefe</baz>
          |		<bat>710-7922</bat>
          |		<field>semper erat, in consectetuer ipsum nunc id enim. Curabitur massa.</field>
          |		<something>467D7085-749F-42B5-DDC1-3F0A70A9CDF5</something>
          |	</record>
          |	<record>
          |		<foo>Gravida Aliquam Tincidunt LLP</foo>
          |		<bar>Christopher P. Williams</bar>
          |		<baz>Sarah</baz>
          |		<bat>1-554-109-7335</bat>
          |		<field>mollis lectus pede et risus. Quisque libero lacus, varius et,</field>
          |		<something>F4B9FFD9-5BDB-BB4A-E0ED-9FAE2659DC2E</something>
          |	</record>
          |	<record>
          |		<foo>Eget Incorporated</foo>
          |		<bar>Fletcher I. Shepherd</bar>
          |		<baz>Kelsey</baz>
          |		<bat>1-577-722-9384</bat>
          |		<field>vel quam dignissim pharetra. Nam ac nulla. In tincidunt congue</field>
          |		<something>F952D6A2-3CD7-7598-B654-6C361C452143</something>
          |	</record>
          |	<record>
          |		<foo>Nunc Mauris Sapien Ltd</foo>
          |		<bar>Burton K. Stafford</bar>
          |		<baz>Amethyst</baz>
          |		<bat>409-1899</bat>
          |		<field>faucibus leo, in lobortis tellus justo sit amet nulla. Donec</field>
          |		<something>9BE397D0-C138-3B02-D6ED-88547299EA33</something>
          |	</record>
          |	<record>
          |		<foo>Est Mauris Rhoncus Ltd</foo>
          |		<bar>Louis G. Downs</bar>
          |		<baz>Stephanie</baz>
          |		<bat>1-106-653-6866</bat>
          |		<field>elit. Curabitur sed tortor. Integer aliquam adipiscing lacus. Ut nec</field>
          |		<something>52863081-0FEE-D1DA-79D1-028D16374960</something>
          |	</record>
          |	<record>
          |		<foo>Dictum LLP</foo>
          |		<bar>Lisandra P. Sanford</bar>
          |		<baz>Bruno</baz>
          |		<bat>848-0680</bat>
          |		<field>scelerisque neque sed sem egestas blandit. Nam nulla magna, malesuada</field>
          |		<something>356CF194-CC18-3673-B001-5D29DB5321BB</something>
          |	</record>
          |	<record>
          |		<foo>Auctor Odio A Limited</foo>
          |		<bar>Christopher D. Baxter</bar>
          |		<baz>Henry</baz>
          |		<bat>679-6748</bat>
          |		<field>Mauris quis turpis vitae purus gravida sagittis. Duis gravida. Praesent</field>
          |		<something>C7D7C2C6-5C5D-612A-2D9B-DCEBAE5CFE10</something>
          |	</record>
          |	<record>
          |		<foo>Vitae Sodales Corp.</foo>
          |		<bar>Lars X. Moody</bar>
          |		<baz>Ebony</baz>
          |		<bat>427-1307</bat>
          |		<field>metus vitae velit egestas lacinia. Sed congue, elit sed consequat</field>
          |		<something>F8EB10FC-6B13-981C-6AF6-7A72E7D34234</something>
          |	</record>
          |	<record>
          |		<foo>Convallis In Inc.</foo>
          |		<bar>Tyrone M. Frank</bar>
          |		<baz>Hedda</baz>
          |		<bat>1-266-568-1897</bat>
          |		<field>Proin vel nisl. Quisque fringilla euismod enim. Etiam gravida molestie</field>
          |		<something>C9B69A4C-5D87-5343-641E-A26420665ED7</something>
          |	</record>
          |	<record>
          |		<foo>Libero Donec Ltd</foo>
          |		<bar>Jayme Q. Garner</bar>
          |		<baz>Mia</baz>
          |		<bat>177-7492</bat>
          |		<field>pellentesque eget, dictum placerat, augue. Sed molestie. Sed id risus</field>
          |		<something>16F8B0EC-4820-BB5F-5374-6A41CE6B769B</something>
          |	</record>
          |	<record>
          |		<foo>Ut Nisi A PC</foo>
          |		<bar>Joel D. Watts</bar>
          |		<baz>Wing</baz>
          |		<bat>794-2232</bat>
          |		<field>nisi dictum augue malesuada malesuada. Integer id magna et ipsum</field>
          |		<something>9B0AA779-8B64-5F77-9BA6-3B5CB3A860B7</something>
          |	</record>
          |	<record>
          |		<foo>Lacus Quisque Purus Associates</foo>
          |		<bar>Kyle Y. Mcdonald</bar>
          |		<baz>Mollie</baz>
          |		<bat>977-0761</bat>
          |		<field>aliquam iaculis, lacus pede sagittis augue, eu tempor erat neque</field>
          |		<something>B077FEE0-4400-C073-2717-84C42CB287A0</something>
          |	</record>
          |	<record>
          |		<foo>Malesuada Id Erat Incorporated</foo>
          |		<bar>Melvin U. Ferguson</bar>
          |		<baz>Price</baz>
          |		<bat>897-7307</bat>
          |		<field>Donec luctus aliquet odio. Etiam ligula tortor, dictum eu, placerat</field>
          |		<something>62F5C285-8D42-3FB4-934A-7C7CFAB91CA2</something>
          |	</record>
          |	<record>
          |		<foo>Nec Eleifend Non LLC</foo>
          |		<bar>Camden D. Campbell</bar>
          |		<baz>Cally</baz>
          |		<bat>1-858-258-6536</bat>
          |		<field>pharetra, felis eget varius ultrices, mauris ipsum porta elit, a</field>
          |		<something>BF9E7AA3-9054-77FD-2A92-E3D3558F524B</something>
          |	</record>
          |	<record>
          |		<foo>Mauris A LLC</foo>
          |		<bar>Meredith R. Fowler</bar>
          |		<baz>Serena</baz>
          |		<bat>1-369-516-3899</bat>
          |		<field>neque. In ornare sagittis felis. Donec tempor, est ac mattis</field>
          |		<something>C5B28343-3506-1BCB-A8AE-0838E8127F8B</something>
          |	</record>
          |	<record>
          |		<foo>Sapien Incorporated</foo>
          |		<bar>Nicholas Y. Mcbride</bar>
          |		<baz>Cailin</baz>
          |		<bat>1-990-845-2761</bat>
          |		<field>commodo ipsum. Suspendisse non leo. Vivamus nibh dolor, nonummy ac,</field>
          |		<something>0C643A77-E35C-65C1-90D0-8BA3BA4BC186</something>
          |	</record>
          |	<record>
          |		<foo>Magna Phasellus Dolor Foundation</foo>
          |		<bar>Vivian S. Daugherty</bar>
          |		<baz>Paul</baz>
          |		<bat>1-966-938-9354</bat>
          |		<field>et magnis dis parturient montes, nascetur ridiculus mus. Proin vel</field>
          |		<something>72768897-C58B-FCB8-C54D-CA931F76D358</something>
          |	</record>
          |	<record>
          |		<foo>Integer Inc.</foo>
          |		<bar>Wilma D. Chase</bar>
          |		<baz>Carlos</baz>
          |		<bat>1-352-797-3406</bat>
          |		<field>magna. Duis dignissim tempor arcu. Vestibulum ut eros non enim</field>
          |		<something>D627560E-5B89-40D6-3DBD-278D71E264C9</something>
          |	</record>
          |	<record>
          |		<foo>Lectus A Sollicitudin Corp.</foo>
          |		<bar>Serina P. Leonard</bar>
          |		<baz>Bevis</baz>
          |		<bat>897-0719</bat>
          |		<field>vel, venenatis vel, faucibus id, libero. Donec consectetuer mauris id</field>
          |		<something>F9421CE8-348C-95E8-368A-F51E3CBC19FB</something>
          |	</record>
          |	<record>
          |		<foo>Est Mauris Eu PC</foo>
          |		<bar>Aimee N. Henry</bar>
          |		<baz>Forrest</baz>
          |		<bat>1-397-462-3698</bat>
          |		<field>egestas. Aliquam nec enim. Nunc ut erat. Sed nunc est,</field>
          |		<something>1208B288-35F0-9CEC-9D20-2D87C22087E1</something>
          |	</record>
          |	<record>
          |		<foo>Sollicitudin Commodo PC</foo>
          |		<bar>Cora D. Ray</bar>
          |		<baz>Curran</baz>
          |		<bat>722-7667</bat>
          |		<field>ullamcorper eu, euismod ac, fermentum vel, mauris. Integer sem elit,</field>
          |		<something>001AD147-A437-2E70-EB3F-C5DBF327BC3F</something>
          |	</record>
          |	<record>
          |		<foo>In Corporation</foo>
          |		<bar>Tashya V. Holden</bar>
          |		<baz>Deirdre</baz>
          |		<bat>1-780-415-1645</bat>
          |		<field>massa. Integer vitae nibh. Donec est mauris, rhoncus id, mollis</field>
          |		<something>2AF25D0C-8A3F-EDCE-D2E9-15BD141F7EE1</something>
          |	</record>
          |	<record>
          |		<foo>Nulla Tempor Consulting</foo>
          |		<bar>Kylie L. Waller</bar>
          |		<baz>Suki</baz>
          |		<bat>704-4553</bat>
          |		<field>blandit congue. In scelerisque scelerisque dui. Suspendisse ac metus vitae</field>
          |		<something>55E83E23-767C-846D-1C84-D2DC95413146</something>
          |	</record>
          |	<record>
          |		<foo>Amet Institute</foo>
          |		<bar>Mallory C. Burke</bar>
          |		<baz>Illiana</baz>
          |		<bat>456-5182</bat>
          |		<field>et, commodo at, libero. Morbi accumsan laoreet ipsum. Curabitur consequat,</field>
          |		<something>C6DA1AB5-8EAE-4BA5-F57A-25A5D0F0AAE4</something>
          |	</record>
          |	<record>
          |		<foo>Vel Faucibus Institute</foo>
          |		<bar>Jamal R. Christensen</bar>
          |		<baz>Colby</baz>
          |		<bat>834-6898</bat>
          |		<field>lacus, varius et, euismod et, commodo at, libero. Morbi accumsan</field>
          |		<something>4A219AD3-E03D-07A5-DB3A-7950B7CCD543</something>
          |	</record>
          |	<record>
          |		<foo>Molestie Incorporated</foo>
          |		<bar>Zenaida J. Campbell</bar>
          |		<baz>Medge</baz>
          |		<bat>509-3463</bat>
          |		<field>dolor. Fusce feugiat. Lorem ipsum dolor sit amet, consectetuer adipiscing</field>
          |		<something>B2FFCA75-DF09-4FD3-20AB-76C3B09D19CB</something>
          |	</record>
          |	<record>
          |		<foo>Velit Eu Sem Corporation</foo>
          |		<bar>Stone R. Cotton</bar>
          |		<baz>Keaton</baz>
          |		<bat>1-766-427-7472</bat>
          |		<field>Curabitur massa. Vestibulum accumsan neque et nunc. Quisque ornare tortor</field>
          |		<something>9D3E43EA-7821-F327-005F-3CCAB0CB7B7E</something>
          |	</record>
          |	<record>
          |		<foo>Aliquam Ltd</foo>
          |		<bar>Quentin C. Tucker</bar>
          |		<baz>Carol</baz>
          |		<bat>1-766-316-4450</bat>
          |		<field>quis diam. Pellentesque habitant morbi tristique senectus et netus et</field>
          |		<something>829BB818-2397-8F67-C7B4-44A3A1FD28FC</something>
          |	</record>
          |	<record>
          |		<foo>Ut Corp.</foo>
          |		<bar>Marah H. Jacobson</bar>
          |		<baz>Chancellor</baz>
          |		<bat>535-0026</bat>
          |		<field>id risus quis diam luctus lobortis. Class aptent taciti sociosqu</field>
          |		<something>4CE079FB-4738-ACEE-C930-62A2E21E4964</something>
          |	</record>
          |	<record>
          |		<foo>Lorem Lorem Consulting</foo>
          |		<bar>Halla N. Kirk</bar>
          |		<baz>Karly</baz>
          |		<bat>1-443-650-7980</bat>
          |		<field>Vestibulum ut eros non enim commodo hendrerit. Donec porttitor tellus</field>
          |		<something>63255DF0-FF22-B44D-D06E-CD78DD3E82FE</something>
          |	</record>
          |	<record>
          |		<foo>At Auctor Ullamcorper Corporation</foo>
          |		<bar>Ruth E. Heath</bar>
          |		<baz>Hilel</baz>
          |		<bat>1-459-621-7647</bat>
          |		<field>odio. Etiam ligula tortor, dictum eu, placerat eget, venenatis a,</field>
          |		<something>68EFFDE9-001D-17AE-0F76-3A305323ED29</something>
          |	</record>
          |	<record>
          |		<foo>Ac Arcu Nunc Ltd</foo>
          |		<bar>Wyoming H. Benson</bar>
          |		<baz>Indigo</baz>
          |		<bat>652-5893</bat>
          |		<field>eu, accumsan sed, facilisis vitae, orci. Phasellus dapibus quam quis</field>
          |		<something>FCC09B7F-50DA-C600-2741-91642653C799</something>
          |	</record>
          |	<record>
          |		<foo>Purus Accumsan Institute</foo>
          |		<bar>Vernon A. Ross</bar>
          |		<baz>Jolie</baz>
          |		<bat>1-258-963-5639</bat>
          |		<field>egestas. Fusce aliquet magna a neque. Nullam ut nisi a</field>
          |		<something>A18CCE24-8EFD-7480-C86A-942E0E98CA3C</something>
          |	</record>
          |	<record>
          |		<foo>Arcu Ac Consulting</foo>
          |		<bar>Barclay J. Madden</bar>
          |		<baz>Uriel</baz>
          |		<bat>246-6107</bat>
          |		<field>urna convallis erat, eget tincidunt dui augue eu tellus. Phasellus</field>
          |		<something>783C0321-BD9B-57C9-5754-C86C7D648014</something>
          |	</record>
          |	<record>
          |		<foo>Quisque Tincidunt Industries</foo>
          |		<bar>Jared L. Glenn</bar>
          |		<baz>Xyla</baz>
          |		<bat>1-376-175-3927</bat>
          |		<field>eros turpis non enim. Mauris quis turpis vitae purus gravida</field>
          |		<something>42734B36-F917-AC7C-5F4C-6730AE481567</something>
          |	</record>
          |	<record>
          |		<foo>Donec Sollicitudin Adipiscing Industries</foo>
          |		<bar>Merritt S. Melton</bar>
          |		<baz>Pascale</baz>
          |		<bat>1-797-777-4294</bat>
          |		<field>tortor at risus. Nunc ac sem ut dolor dapibus gravida.</field>
          |		<something>8B995247-B732-2C83-23AE-35CE70E1E01C</something>
          |	</record>
          |	<record>
          |		<foo>Nec Incorporated</foo>
          |		<bar>Francesca M. Walsh</bar>
          |		<baz>Holly</baz>
          |		<bat>1-798-405-7357</bat>
          |		<field>arcu. Nunc mauris. Morbi non sapien molestie orci tincidunt adipiscing.</field>
          |		<something>DFCDC4E3-85F3-C1A3-97F5-66BDA8AFC496</something>
          |	</record>
          |	<record>
          |		<foo>Ut Sagittis Institute</foo>
          |		<bar>Elaine E. Waters</bar>
          |		<baz>Sybil</baz>
          |		<bat>226-2352</bat>
          |		<field>diam dictum sapien. Aenean massa. Integer vitae nibh. Donec est</field>
          |		<something>02FC6399-17C5-79F1-017A-CD2D816A5760</something>
          |	</record>
          |	<record>
          |		<foo>Lorem Lorem Consulting</foo>
          |		<bar>Carla V. Vang</bar>
          |		<baz>Aaron</baz>
          |		<bat>516-9582</bat>
          |		<field>lectus, a sollicitudin orci sem eget massa. Suspendisse eleifend. Cras</field>
          |		<something>98875B58-4667-E298-A16A-795245161A94</something>
          |	</record>
          |	<record>
          |		<foo>Velit Industries</foo>
          |		<bar>Winter T. Wilkins</bar>
          |		<baz>Jacqueline</baz>
          |		<bat>436-1880</bat>
          |		<field>Suspendisse aliquet molestie tellus. Aenean egestas hendrerit neque. In ornare</field>
          |		<something>6E50D7AC-34C5-E056-EF96-619F6BDDA6DD</something>
          |	</record>
          |	<record>
          |		<foo>Fringilla PC</foo>
          |		<bar>Veda I. Crawford</bar>
          |		<baz>Ezekiel</baz>
          |		<bat>380-3299</bat>
          |		<field>sit amet luctus vulputate, nisi sem semper erat, in consectetuer</field>
          |		<something>2CADB8F5-94A2-D755-4931-15C4F7FC1CC1</something>
          |	</record>
          |	<record>
          |		<foo>Arcu Institute</foo>
          |		<bar>Harriet M. Rios</bar>
          |		<baz>Damian</baz>
          |		<bat>319-6766</bat>
          |		<field>enim. Suspendisse aliquet, sem ut cursus luctus, ipsum leo elementum</field>
          |		<something>D3BF0960-0913-9863-CE8A-8B68DD39FAFD</something>
          |	</record>
          |	<record>
          |		<foo>Tristique Senectus Et Ltd</foo>
          |		<bar>Berk Z. Jarvis</bar>
          |		<baz>Montana</baz>
          |		<bat>1-666-629-0576</bat>
          |		<field>a, aliquet vel, vulputate eu, odio. Phasellus at augue id</field>
          |		<something>5B236B29-787A-C34F-163E-BDFC6911FA46</something>
          |	</record>
          |	<record>
          |		<foo>Ultrices Sit Amet Foundation</foo>
          |		<bar>Imani E. Mays</bar>
          |		<baz>Miriam</baz>
          |		<bat>1-760-538-9274</bat>
          |		<field>lacus, varius et, euismod et, commodo at, libero. Morbi accumsan</field>
          |		<something>DCC44F73-CA95-CB5B-2FEC-B79FB0FE1B4A</something>
          |	</record>
          |	<record>
          |		<foo>Sed Ltd</foo>
          |		<bar>Rinah O. Mitchell</bar>
          |		<baz>Elaine</baz>
          |		<bat>1-576-542-0105</bat>
          |		<field>elit. Aliquam auctor, velit eget laoreet posuere, enim nisl elementum</field>
          |		<something>D9249F87-63DA-FB57-71AA-CFA1E4C58BB5</something>
          |	</record>
          |	<record>
          |		<foo>Tristique Associates</foo>
          |		<bar>Inez G. Ellison</bar>
          |		<baz>Ferdinand</baz>
          |		<bat>338-6690</bat>
          |		<field>congue, elit sed consequat auctor, nunc nulla vulputate dui, nec</field>
          |		<something>9EC282BE-CCF5-F797-D66F-F3CE0978B339</something>
          |	</record>
          |	<record>
          |		<foo>Elit Nulla Facilisi Institute</foo>
          |		<bar>May O. Hardy</bar>
          |		<baz>Alisa</baz>
          |		<bat>1-556-460-9235</bat>
          |		<field>id, libero. Donec consectetuer mauris id sapien. Cras dolor dolor,</field>
          |		<something>1D1CC1F5-FBB4-6C53-2564-6CB846B42238</something>
          |	</record>
          |	<record>
          |		<foo>Eleifend Nec Institute</foo>
          |		<bar>Raya C. Barton</bar>
          |		<baz>Lamar</baz>
          |		<bat>1-652-804-7538</bat>
          |		<field>dui. Suspendisse ac metus vitae velit egestas lacinia. Sed congue,</field>
          |		<something>A63C3D9D-66E9-0F33-31D0-C7277B49E99E</something>
          |	</record>
          |	<record>
          |		<foo>Ante Ipsum Industries</foo>
          |		<bar>Allistair Z. Mcgowan</bar>
          |		<baz>Briar</baz>
          |		<bat>1-944-215-2010</bat>
          |		<field>ridiculus mus. Donec dignissim magna a tortor. Nunc commodo auctor</field>
          |		<something>7B8F117F-923E-0494-F95F-0FE37547B869</something>
          |	</record>
          |	<record>
          |		<foo>Magna Cras Consulting</foo>
          |		<bar>Jenette A. Powell</bar>
          |		<baz>Galena</baz>
          |		<bat>728-3948</bat>
          |		<field>imperdiet ornare. In faucibus. Morbi vehicula. Pellentesque tincidunt tempus risus.</field>
          |		<something>FDFFE87B-CB89-1B5F-08FD-A0F2DB3F6780</something>
          |	</record>
          |	<record>
          |		<foo>Et Netus Et Incorporated</foo>
          |		<bar>Brynne M. Miles</bar>
          |		<baz>Byron</baz>
          |		<bat>1-777-452-5870</bat>
          |		<field>nulla at sem molestie sodales. Mauris blandit enim consequat purus.</field>
          |		<something>5902D85B-41F7-A3FB-3B43-701C6D7A66B0</something>
          |	</record>
          |	<record>
          |		<foo>Sem Vitae Aliquam Foundation</foo>
          |		<bar>Ivy Z. Barnes</bar>
          |		<baz>Ronan</baz>
          |		<bat>201-7218</bat>
          |		<field>diam. Sed diam lorem, auctor quis, tristique ac, eleifend vitae,</field>
          |		<something>91EC425C-1CA7-5382-4CA4-403E3DE6706C</something>
          |	</record>
          |	<record>
          |		<foo>Ullamcorper Velit In Company</foo>
          |		<bar>Emi G. Sampson</bar>
          |		<baz>Amena</baz>
          |		<bat>367-8648</bat>
          |		<field>faucibus orci luctus et ultrices posuere cubilia Curae; Donec tincidunt.</field>
          |		<something>05037532-865E-DEE0-F0AE-252B7DC6457D</something>
          |	</record>
          |	<record>
          |		<foo>Eu Corporation</foo>
          |		<bar>Lionel N. Sweet</bar>
          |		<baz>Indira</baz>
          |		<bat>1-904-755-9502</bat>
          |		<field>pede blandit congue. In scelerisque scelerisque dui. Suspendisse ac metus</field>
          |		<something>6CADA523-6FF8-AD2B-9F58-6F7373651640</something>
          |	</record>
          |	<record>
          |		<foo>Dolor Company</foo>
          |		<bar>Ria Y. Carey</bar>
          |		<baz>Reagan</baz>
          |		<bat>457-7041</bat>
          |		<field>nibh. Aliquam ornare, libero at auctor ullamcorper, nisl arcu iaculis</field>
          |		<something>A8301F03-1D77-6C0A-5585-B2F830164B11</something>
          |	</record>
          |	<record>
          |		<foo>A Incorporated</foo>
          |		<bar>Taylor H. Poole</bar>
          |		<baz>Jerome</baz>
          |		<bat>589-0672</bat>
          |		<field>egestas hendrerit neque. In ornare sagittis felis. Donec tempor, est</field>
          |		<something>5596E0A8-AAD1-8B8F-BC2C-BD7217959B4C</something>
          |	</record>
          |	<record>
          |		<foo>Vulputate Risus A LLC</foo>
          |		<bar>Alea K. Fischer</bar>
          |		<baz>Clark</baz>
          |		<bat>670-1324</bat>
          |		<field>enim. Sed nulla ante, iaculis nec, eleifend non, dapibus rutrum,</field>
          |		<something>6591B61D-56C1-019E-318A-B783B1530104</something>
          |	</record>
          |	<record>
          |		<foo>Cras Lorem Foundation</foo>
          |		<bar>Emi J. Oliver</bar>
          |		<baz>Callum</baz>
          |		<bat>834-1316</bat>
          |		<field>tellus faucibus leo, in lobortis tellus justo sit amet nulla.</field>
          |		<something>F78319D2-2778-08E1-DB0E-FECBBF58B264</something>
          |	</record>
          |	<record>
          |		<foo>Luctus Et Ultrices Consulting</foo>
          |		<bar>Palmer O. Ruiz</bar>
          |		<baz>Roth</baz>
          |		<bat>1-856-941-1216</bat>
          |		<field>sit amet metus. Aliquam erat volutpat. Nulla facilisis. Suspendisse commodo</field>
          |		<something>26088222-B8B7-2164-9B88-718EF28D0659</something>
          |	</record>
          |	<record>
          |		<foo>Eu LLC</foo>
          |		<bar>Jorden J. Owens</bar>
          |		<baz>Whitney</baz>
          |		<bat>318-0404</bat>
          |		<field>Cras vehicula aliquet libero. Integer in magna. Phasellus dolor elit,</field>
          |		<something>C47E3EF5-BDE5-5741-8CC6-B482D0C61D48</something>
          |	</record>
          |	<record>
          |		<foo>Nulla Integer Institute</foo>
          |		<bar>Wilma T. Kirk</bar>
          |		<baz>Shad</baz>
          |		<bat>385-2065</bat>
          |		<field>ornare. Fusce mollis. Duis sit amet diam eu dolor egestas</field>
          |		<something>D0887931-A8AE-6058-D252-EAD29F251B75</something>
          |	</record>
          |	<record>
          |		<foo>Purus Nullam Scelerisque Corporation</foo>
          |		<bar>Nolan Y. Meadows</bar>
          |		<baz>Anthony</baz>
          |		<bat>331-5596</bat>
          |		<field>egestas. Duis ac arcu. Nunc mauris. Morbi non sapien molestie</field>
          |		<something>01026343-C88A-349E-2ECF-142E6D540E32</something>
          |	</record>
          |	<record>
          |		<foo>Sed Eget LLP</foo>
          |		<bar>Madonna D. Best</bar>
          |		<baz>Jeanette</baz>
          |		<bat>1-269-113-0208</bat>
          |		<field>egestas blandit. Nam nulla magna, malesuada vel, convallis in, cursus</field>
          |		<something>810EEF36-CDA3-AA5C-E348-58309D96FB58</something>
          |	</record>
          |	<record>
          |		<foo>Ipsum Non LLP</foo>
          |		<bar>Oleg X. Morton</bar>
          |		<baz>Taylor</baz>
          |		<bat>794-2105</bat>
          |		<field>vel, mauris. Integer sem elit, pharetra ut, pharetra sed, hendrerit</field>
          |		<something>477167B6-D4B0-3B4A-3EAF-2C4BA2199977</something>
          |	</record>
          |	<record>
          |		<foo>Ultrices Posuere Cubilia LLP</foo>
          |		<bar>Leonard A. Levy</bar>
          |		<baz>Hakeem</baz>
          |		<bat>301-9608</bat>
          |		<field>Nulla semper tellus id nunc interdum feugiat. Sed nec metus</field>
          |		<something>3C341709-174F-D656-63CB-ADC6CBE0011A</something>
          |	</record>
          |	<record>
          |		<foo>Fringilla Cursus Consulting</foo>
          |		<bar>Aiko C. Lang</bar>
          |		<baz>Rashad</baz>
          |		<bat>524-5258</bat>
          |		<field>Duis mi enim, condimentum eget, volutpat ornare, facilisis eget, ipsum.</field>
          |		<something>5637193E-90D8-8FBD-006A-2641088478BA</something>
          |	</record>
          |	<record>
          |		<foo>Tellus Institute</foo>
          |		<bar>Allegra C. Short</bar>
          |		<baz>Nola</baz>
          |		<bat>1-936-876-9724</bat>
          |		<field>semper cursus. Integer mollis. Integer tincidunt aliquam arcu. Aliquam ultrices</field>
          |		<something>2970E4B6-81B9-00DD-3B0A-30B84FFC35F6</something>
          |	</record>
          |	<record>
          |		<foo>A Inc.</foo>
          |		<bar>Zahir K. Jones</bar>
          |		<baz>Ashton</baz>
          |		<bat>283-2418</bat>
          |		<field>sit amet ornare lectus justo eu arcu. Morbi sit amet</field>
          |		<something>CD478302-5A4E-89AF-2028-72FC84CF0547</something>
          |	</record>
          |	<record>
          |		<foo>Rutrum Fusce Institute</foo>
          |		<bar>Marah M. Sweet</bar>
          |		<baz>Rana</baz>
          |		<bat>748-6648</bat>
          |		<field>nec ante. Maecenas mi felis, adipiscing fringilla, porttitor vulputate, posuere</field>
          |		<something>832F0374-25F4-E492-37FE-AEFB08EC9049</something>
          |	</record>
          |	<record>
          |		<foo>Eu Tempor Erat Associates</foo>
          |		<bar>Ruby T. Bowers</bar>
          |		<baz>Plato</baz>
          |		<bat>186-7475</bat>
          |		<field>ac, eleifend vitae, erat. Vivamus nisi. Mauris nulla. Integer urna.</field>
          |		<something>AB958C64-6AFD-DFC5-D9E2-8AFE816A9EB2</something>
          |	</record>
          |	<record>
          |		<foo>Arcu Iaculis Enim Associates</foo>
          |		<bar>Vaughan M. Atkins</bar>
          |		<baz>Taylor</baz>
          |		<bat>1-474-333-7275</bat>
          |		<field>nec luctus felis purus ac tellus. Suspendisse sed dolor. Fusce</field>
          |		<something>9618B00C-114B-0959-C93F-1147414486F0</something>
          |	</record>
          |	<record>
          |		<foo>Vitae Sodales Nisi Incorporated</foo>
          |		<bar>Ruth J. Walter</bar>
          |		<baz>Lavinia</baz>
          |		<bat>967-0364</bat>
          |		<field>Ut sagittis lobortis mauris. Suspendisse aliquet molestie tellus. Aenean egestas</field>
          |		<something>E8F523DF-5405-2F34-E799-DAF8504D3BCD</something>
          |	</record>
          |	<record>
          |		<foo>Enim Mauris Corp.</foo>
          |		<bar>Hilel L. Christensen</bar>
          |		<baz>Kyra</baz>
          |		<bat>1-670-846-6559</bat>
          |		<field>lorem, luctus ut, pellentesque eget, dictum placerat, augue. Sed molestie.</field>
          |		<something>288E84D5-C339-EF9B-5400-56BFC3DCDADF</something>
          |	</record>
          |	<record>
          |		<foo>Elementum Purus Accumsan PC</foo>
          |		<bar>Sonya J. Franco</bar>
          |		<baz>Imogene</baz>
          |		<bat>250-8987</bat>
          |		<field>non, egestas a, dui. Cras pellentesque. Sed dictum. Proin eget</field>
          |		<something>BD673479-C49F-B145-9BE9-895F7D8A480B</something>
          |	</record>
          |	<record>
          |		<foo>Lobortis Industries</foo>
          |		<bar>Suki B. Welch</bar>
          |		<baz>Ferdinand</baz>
          |		<bat>460-6008</bat>
          |		<field>est mauris, rhoncus id, mollis nec, cursus a, enim. Suspendisse</field>
          |		<something>EACBBE6C-AC59-F45D-C2A5-31CD1832CF7F</something>
          |	</record>
          |	<record>
          |		<foo>Aliquet Corp.</foo>
          |		<bar>Ishmael S. Bennett</bar>
          |		<baz>Jerry</baz>
          |		<bat>697-3951</bat>
          |		<field>vulputate eu, odio. Phasellus at augue id ante dictum cursus.</field>
          |		<something>F2F7BF8F-5D78-6FD9-3773-596FE2C991F7</something>
          |	</record>
          |</records>
        """.stripMargin)
  }

  /**
   * An example of how to serve files or an index. If the path param of "*" matches the name/path
   * of a file that can be resolved by the [[com.twitter.finatra.utils.FileResolver]]
   * then the file will be returned. Otherwise the file at 'indexPath' (in this case 'index.html')
   * will be returned. This is useful for building "single-page" web applications.
   *
   * Routes a are matched in the order they are defined, thus this route SHOULD be LAST as it is
   * a "catch-all" and routes should be defined in order of most-specific to least-specific.
   *
   * @see https://twitter.github.io/finatra/user-guide/build-new-http-server/controller.html#controllers-and-routing
   * @see https://twitter.github.io/finatra/user-guide/files/
   */
  get("/:*") { request: Request =>
    response.ok.fileOrIndex(filePath = request.params("*"), indexPath = "index.html")
  }
}
