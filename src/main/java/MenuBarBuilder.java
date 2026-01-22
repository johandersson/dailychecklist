/*
 * Daily Checklist
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class MenuBarBuilder {
    public static JMenuBar build(java.awt.Component parent, TaskManager taskManager, Runnable updateTasks) {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> {
            if (taskManager.hasUndoneTasks()) {
                int response = JOptionPane.showConfirmDialog(parent,
                        "There are undone tasks. Are you sure you want to exit?",
                        "Confirm Exit",
                        JOptionPane.YES_NO_OPTION);
                if (response != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            System.exit(0);
        });
        fileMenu.add(exitItem);

        //add about menu item
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> {
            JDialog aboutDialog = new JDialog((java.awt.Frame) parent, "About Daily Checklist", true);
            aboutDialog.setLayout(new BorderLayout());
            aboutDialog.setSize(600, 800);
            aboutDialog.setLocationRelativeTo(parent);
            aboutDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            // Title panel
            JPanel titlePanel = new JPanel();
            titlePanel.setBackground(new Color(0, 123, 255)); // Bootstrap blue
            JLabel titleLabel = new JLabel("Daily Checklist");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
            titleLabel.setForeground(Color.WHITE);
            titlePanel.add(titleLabel);
            aboutDialog.add(titlePanel, BorderLayout.NORTH);

            // Content panel
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            contentPanel.setBackground(Color.WHITE);
            String gpl3Text = "                    GNU GENERAL PUBLIC LICENSE\n" +
                "                       Version 3, 29 June 2007\n" +
                "\n" +
                " Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>\n" +
                " Everyone is permitted to copy and distribute verbatim copies\n" +
                " of this license document, but changing it is not allowed.\n" +
                "\n" +
                "                            Preamble\n" +
                "\n" +
                "  The GNU General Public License is a free, copyleft license for\n" +
                "software and other kinds of works.\n" +
                "\n" +
                "  The licenses for most software and other practical works are\n" +
                "designed to take away your freedom to share and change the works.\n" +
                "  By contrast, the GNU General Public License is intended to\n" +
                "guarantee your freedom to share and change all versions of a\n" +
                "program--to make sure it remains free software for all its users.\n" +
                "  We, the Free Software Foundation, use the GNU General Public\n" +
                "License for most of our software; it applies also to any other work\n" +
                "released this way by its authors.  You can apply it to your\n" +
                "programs, too.\n" +
                "\n" +
                "  When we speak of free software, we are referring to freedom, not\n" +
                "price.  Our General Public Licenses are designed to make sure that\n" +
                "you have the freedom to distribute copies of free software (and\n" +
                "charge for them if you wish), that you receive source code or can\n" +
                "get it if you want it, that you can change the software or use\n" +
                "pieces of it in new free programs, and that you know you can do\n" +
                "these things.\n" +
                "\n" +
                "  To protect your rights, we need to prevent others from denying\n" +
                "you these rights or asking you to surrender the rights.\n" +
                "  Therefore, you have certain responsibilities if you distribute\n" +
                "copies of the software, or if you modify it: responsibilities to\n" +
                "respect the freedom of others.\n" +
                "\n" +
                "  For example, if you distribute copies of such a program, whether\n" +
                "gratis or for a fee, you must pass on to the recipients the same\n" +
                "freedoms that you received.  You must make sure that they, too,\n" +
                "receive or can get the source code.  And you must show them these\n" +
                "terms so they know their rights.\n" +
                "\n" +
                "  Developers that use the GNU GPL protect your rights with two\n" +
                "steps: (1) assert copyright on the software, and (2) offer you\n" +
                "this License giving you legal permission to copy, distribute\n" +
                "and/or modify it.\n" +
                "\n" +
                "  For the developers' and authors' protection, the GPL clearly\n" +
                "explains that there is no warranty for this free software.  For\n" +
                "both users' and authors' sake, the GPL requires that modified\n" +
                "versions be marked as changed, so that their problems will not be\n" +
                "attributed erroneously to authors of previous versions.\n" +
                "\n" +
                "  Some devices are designed to deny users access to install or run\n" +
                "modified versions of the software inside them, although the\n" +
                "manufacturer can do so.  This is fundamentally incompatible with\n" +
                "the aim of protecting users' freedom to change the software.  The\n" +
                "systematic pattern of such abuse occurs in the area of products\n" +
                "for individuals to use, which is precisely where it is most\n" +
                "unacceptable.  Therefore, we have designed this version of the GPL\n" +
                "to prohibit the practice for those products.  If such problems\n" +
                "arise substantially in other domains, we stand ready to extend\n" +
                "this provision to those domains in future versions of the GPL, as\n" +
                "needed to protect the freedom of users.\n" +
                "\n" +
                "  Finally, every program is threatened constantly by software\n" +
                "patents.  States should not allow patents to restrict development\n" +
                "and use of software on general-purpose computers, but in those\n" +
                "that do, we wish to avoid the special danger that patents applied\n" +
                "to a free program could make it effectively proprietary.  To\n" +
                "prevent this, the GPL assures that patents cannot be used to\n" +
                "render the program non-free.\n" +
                "\n" +
                "  The precise terms and conditions for copying, distribution and\n" +
                "modification follow.\n" +
                "\n" +
                "                       TERMS AND CONDITIONS\n" +
                "\n" +
                "  0. Definitions.\n" +
                "\n" +
                "  \"This License\" refers to version 3 of the GNU General Public\n" +
                "License.\n" +
                "\n" +
                "  \"Copyright\" also means copyright-like laws that apply to other\n" +
                "kinds of works, such as semiconductor masks.\n" +
                "\n" +
                "  \"The Program\" refers to any copyrightable work licensed under\n" +
                "this License.  Each licensee is addressed as \"you\". \"Licensees\"\n" +
                "and \"recipients\" may be individuals or organizations.\n" +
                "\n" +
                "  To \"modify\" a work means to copy from or adapt all or part of\n" +
                "the work in a fashion requiring copyright permission, other than\n" +
                "the making of an exact copy.  The resulting work is called a\n" +
                "\"modified version\" of the earlier work or a work \"based on\" the\n" +
                "earlier work.\n" +
                "\n" +
                "  A \"covered work\" means either the unmodified Program or a work\n" +
                "based on the Program.\n" +
                "\n" +
                "  To \"propagate\" a work means to do anything with it that, without\n" +
                "permission, would make you directly or secondarily liable for\n" +
                "infringement under applicable copyright law, except executing it\n" +
                "on a computer or modifying a private copy.  Propagation includes\n" +
                "copying, distribution (with or without modification), making\n" +
                "available to the public, and in some countries other activities\n" +
                "as well.\n" +
                "\n" +
                "  To \"convey\" a work means any kind of propagation that enables\n" +
                "other parties to make or receive copies.  Mere interaction with a\n" +
                "user through a computer network, with no transfer of a copy, is\n" +
                "not conveying.\n" +
                "\n" +
                "  An interactive user interface displays \"Appropriate Legal\n" +
                "Notices\" to the extent that it includes a convenient and\n" +
                "prominently visible feature that (1) displays an appropriate\n" +
                "copyright notice, and (2) tells the user that there is no\n" +
                "warranty for the work (except to the extent that warranties are\n" +
                "provided), that licensees may convey the work under this License,\n" +
                "and how to view a copy of this License.  If the interface\n" +
                "presents a list of user commands or options, such as a menu, a\n" +
                "prominent item in the list meets this criterion.\n" +
                "\n" +
                "  1. Source Code.\n" +
                "\n" +
                "  The \"source code\" for a work means the preferred form of the\n" +
                "work for making modifications to it. \"Object code\" means any\n" +
                "non-source form of a work.\n" +
                "\n" +
                "  A \"Standard Interface\" means an interface that either is an\n" +
                "official standard defined by a recognized standards body, or, in\n" +
                "the case of interfaces specified for a particular programming\n" +
                "language, one that is widely used among developers working in\n" +
                "that language.\n" +
                "\n" +
                "  The \"System Libraries\" of an executable work include anything,\n" +
                "other than the work as a whole, that (a) is included in the\n" +
                "normal form of packaging a Major Component, but which is not\n" +
                "part of that Major Component, and (b) serves only to enable use\n" +
                "of the work with that Major Component, or to implement a\n" +
                "Standard Interface for which an implementation is available to\n" +
                "the public in source code form. A \"Major Component\", in this\n" +
                "context, means a major essential component (kernel, window\n" +
                "system, and so on) of the specific operating system (if any) on\n" +
                "which the executable work runs, or a compiler used to produce\n" +
                "the work, or an object code interpreter used to run it.\n" +
                "\n" +
                "  The \"Corresponding Source\" for a work in object code form\n" +
                "means all the source code needed to generate, install, and (for\n" +
                "an executable work) run the object code and to modify the work,\n" +
                "including scripts to control those activities.  However, it does\n" +
                "not include the work's System Libraries, or general-purpose\n" +
                "tools or generally available free programs which are used\n" +
                "unmodified in performing those activities but which are not\n" +
                "part of the work.  For example, Corresponding Source includes\n" +
                "interface definition files associated with source files for the\n" +
                "work, and the source code for shared libraries and dynamically\n" +
                "linked subprograms that the work is specifically designed to\n" +
                "require, such as by intimate data communication or control flow\n" +
                "between those subprograms and other parts of the work.\n" +
                "\n" +
                "  The Corresponding Source need not include anything that users\n" +
                "can regenerate automatically from other parts of the\n" +
                "Corresponding Source.\n" +
                "\n" +
                "  The Corresponding Source for a work in source code form is\n" +
                "that same work.\n" +
                "\n" +
                "  2. Basic Permissions.\n" +
                "\n" +
                "  All rights granted under this License are granted for the term\n" +
                "of copyright on the Program, and are irrevocable provided the\n" +
                "stated conditions are met.  This License explicitly affirms\n" +
                "your unlimited permission to run the unmodified Program.  The\n" +
                "output from running a covered work is covered by this License\n" +
                "only if the output, given its content, constitutes a covered\n" +
                "work.  This License acknowledges your rights of fair use or\n" +
                "other equivalent, as provided by copyright law.\n" +
                "\n" +
                "  You may make, run and propagate covered works that you do not\n" +
                "convey, without conditions so long as your license otherwise\n" +
                "remains in force.  You may convey covered works to others for\n" +
                "the sole purpose of having them make modifications exclusively\n" +
                "for you, or provide you with facilities for running those works,\n" +
                "provided that you comply with the terms of this License in\n" +
                "conveying all material for which you do not control copyright.\n" +
                "  Those thus making or running the covered works for you must\n" +
                "do so exclusively on your behalf, under your direction and\n" +
                "control, on terms that prohibit them from making any copies of\n" +
                "your copyrighted material outside their relationship with you.\n" +
                "\n" +
                "  Conveying under any other circumstances is permitted solely\n" +
                "under the conditions stated below.  Sublicensing is not allowed;\n" +
                "section 10 makes it unnecessary.\n" +
                "\n" +
                "  3. Protecting Users' Legal Rights From Anti-Circumvention Law.\n" +
                "\n" +
                "  No covered work shall be deemed part of an effective\n" +
                "technological measure under any applicable law fulfilling\n" +
                "obligations under article 11 of the WIPO copyright treaty\n" +
                "adopted on 20 December 1996, or similar laws prohibiting or\n" +
                "restricting circumvention of such measures.\n" +
                "\n" +
                "  When you convey a covered work, you waive any legal power to\n" +
                "forbid circumvention of technological measures to the extent\n" +
                "such circumvention is effected by exercising rights under this\n" +
                "License with respect to the covered work, and you disclaim any\n" +
                "intention to limit operation or modification of the work as a\n" +
                "means of enforcing, against the work's users, your or third\n" +
                "parties' legal rights to forbid circumvention of technological\n" +
                "measures.\n" +
                "\n" +
                "  4. Conveying Verbatim Copies.\n" +
                "\n" +
                "  You may convey verbatim copies of the Program's source code as\n" +
                "you receive it, in any medium, provided that you conspicuously\n" +
                "and appropriately publish on each copy an appropriate copyright\n" +
                "notice; keep intact all notices stating that this License and\n" +
                "any non-permissive terms added in accord with section 7 apply\n" +
                "to the code; keep intact all notices of the absence of any\n" +
                "warranty; and give all recipients a copy of this License along\n" +
                "with the Program.\n" +
                "\n" +
                "  You may charge any price or no price for each copy that you\n" +
                "convey, and you may offer support or warranty protection for a\n" +
                "fee.\n" +
                "\n" +
                "  5. Conveying Modified Source Versions.\n" +
                "\n" +
                "  You may convey a work based on the Program, or the\n" +
                "modifications to produce it from the Program, in the form of\n" +
                "source code under the terms of section 4, provided that you\n" +
                "also meet all of these conditions:\n" +
                "\n" +
                "    a) The work must carry prominent notices stating that you\n" +
                "    modified it, and giving a relevant date.\n" +
                "\n" +
                "    b) The work must carry prominent notices stating that it is\n" +
                "    released under this License and any conditions added under\n" +
                "    section 7.  This requirement modifies the requirement in\n" +
                "    section 4 to \"keep intact all notices\".\n" +
                "\n" +
                "    c) You must license the entire work, as a whole, under this\n" +
                "    License to anyone who comes into possession of a copy.  This\n" +
                "    License will therefore apply, along with any applicable\n" +
                "    section 7 additional terms, to the whole of the work, and\n" +
                "    all its parts, regardless of how they are packaged.  This\n" +
                "    License gives no permission to license the work in any other\n" +
                "    way, but it does not invalidate such permission if you have\n" +
                "    separately received it.\n" +
                "\n" +
                "    d) If the work has interactive user interfaces, each must\n" +
                "    display Appropriate Legal Notices; however, if the Program\n" +
                "    has interactive interfaces that do not display Appropriate\n" +
                "    Legal Notices, your work need not make them do so.\n" +
                "\n" +
                "  A compilation of a covered work with other separate and\n" +
                "independent works, which are not by their nature extensions of\n" +
                "the covered work, and which are not combined with it such as to\n" +
                "form a larger program, in or on a volume of a storage or\n" +
                "distribution medium, is called an \"aggregate\" if the\n" +
                "compilation and its resulting copyright are not used to limit\n" +
                "the access or legal rights of the compilation's users beyond\n" +
                "what the individual works permit.  Inclusion of a covered work\n" +
                "in an aggregate does not cause this License to apply to the\n" +
                "other parts of the aggregate.\n" +
                "\n" +
                "  6. Conveying Non-Source Forms.\n" +
                "\n" +
                "  You may convey a covered work in object code form under the\n" +
                "terms of sections 4 and 5, provided that you also convey the\n" +
                "machine-readable Corresponding Source under the terms of this\n" +
                "License, in one of these ways:\n" +
                "\n" +
                "    a) Convey the object code in, or embodied in, a physical\n" +
                "    product (including a physical distribution medium),\n" +
                "    accompanied by the Corresponding Source fixed on a durable\n" +
                "    physical medium customarily used for software interchange.\n" +
                "\n" +
                "    b) Convey the object code in, or embodied in, a physical\n" +
                "    product (including a physical distribution medium),\n" +
                "    accompanied by a written offer, valid for at least three\n" +
                "    years and valid for as long as you offer spare parts or\n" +
                "    customer support for that product model, to give anyone who\n" +
                "    possesses the object code either (1) a copy of the\n" +
                "    Corresponding Source for all the software in the product\n" +
                "    that is covered by this License, on a durable physical\n" +
                "    medium customarily used for software interchange, for a\n" +
                "    price no more than your reasonable cost of physically\n" +
                "    performing this conveying of source, or (2) access to copy\n" +
                "    the Corresponding Source from a network server at no charge.\n" +
                "\n" +
                "    c) Convey individual copies of the object code with a copy\n" +
                "    of the written offer to provide the Corresponding Source.\n" +
                "    This alternative is allowed only occasionally and\n" +
                "    noncommercially, and only if you received the object code\n" +
                "    with such an offer, in accord with subsection 6b.\n" +
                "\n" +
                "    d) Convey the object code by offering access from a\n" +
                "    designated place (gratis or for a charge), and offer\n" +
                "    equivalent access to the Corresponding Source in the same\n" +
                "    way through the same place at no further charge.  You need\n" +
                "    not require recipients to copy the Corresponding Source\n" +
                "    along with the object code.  If the place to copy the\n" +
                "    object code is a network server, the Corresponding Source\n" +
                "    may be on a different server (operated by you or a third\n" +
                "    party) that supports equivalent copying facilities,\n" +
                "    provided you maintain clear directions next to the object\n" +
                "    code saying where to find the Corresponding Source.\n" +
                "    Regardless of what server hosts the Corresponding Source,\n" +
                "    you remain obligated to ensure that it is available for as\n" +
                "    long as needed to satisfy these requirements.\n" +
                "\n" +
                "    e) Convey the object code using peer-to-peer transmission,\n" +
                "    provided you inform other peers where the object code and\n" +
                "    Corresponding Source of the work are being offered to the\n" +
                "    general public at no charge under subsection 6d.\n" +
                "\n" +
                "  A separable portion of the object code, whose source code is\n" +
                "excluded from the Corresponding Source as a System Library,\n" +
                "need not be included in conveying the object code work.\n" +
                "\n" +
                "  A \"User Product\" is either (1) a \"consumer product\", which\n" +
                "means any tangible personal property which is normally used for\n" +
                "personal, family, or household purposes, or (2) anything\n" +
                "designed or sold for incorporation into a dwelling.  In\n" +
                "determining whether a product is a consumer product, doubtful\n" +
                "cases shall be resolved in favor of coverage.  For a particular\n" +
                "product received by a particular user, \"normally used\" refers\n" +
                "to a typical or common use of that class of product, regardless\n" +
                "of the status of the particular user or of the way in which the\n" +
                "particular user actually uses, or expects or is expected to\n" +
                "use, the product.  A product is a consumer product regardless\n" +
                "of whether the product has substantial commercial, industrial\n" +
                "or non-consumer uses, unless such uses represent the only\n" +
                "significant mode of use of the product.\n" +
                "\n" +
                "  \"Installation Information\" for a User Product means any\n" +
                "methods, procedures, authorization keys, or other information\n" +
                "required to install and execute modified versions of a covered\n" +
                "work in that User Product from a modified version of its\n" +
                "Corresponding Source.  The information must suffice to ensure\n" +
                "that the continued functioning of the modified object code is\n" +
                "in no case prevented or interfered with solely because\n" +
                "modification has been made.\n" +
                "\n" +
                "  If you convey an object code work under this section in, or\n" +
                "with, or specifically for use in, a User Product, and the\n" +
                "conveying occurs as part of a transaction in which the right of\n" +
                "possession and use of the User Product is transferred to the\n" +
                "recipient in perpetuity or for a fixed term (regardless of how\n" +
                "the transaction is characterized), the Corresponding Source\n" +
                "conveyed under this section must be accompanied by the\n" +
                "Installation Information.  But this requirement does not apply\n" +
                "if neither you nor any third party retains the ability to\n" +
                "install modified object code on the User Product (for example,\n" +
                "the work has been installed in ROM).\n" +
                "\n" +
                "  The requirement to provide Installation Information does not\n" +
                "include a requirement to continue to provide support service,\n" +
                "warranty, or updates for a work that has been modified or\n" +
                "installed by the recipient, or for the User Product in which it\n" +
                "has been modified or installed.  Access to a network may be\n" +
                "denied when the modification itself materially and adversely\n" +
                "affects the operation of the network or violates the rules and\n" +
                "protocols for communication across the network.\n" +
                "\n" +
                "  Corresponding Source conveyed, and Installation Information\n" +
                "provided, in accord with this section must be in a format that\n" +
                "is publicly documented (and with an implementation available to\n" +
                "the public in source code form), and must require no special\n" +
                "password or key for unpacking, reading or copying.\n" +
                "\n" +
                "  7. Additional Terms.\n" +
                "\n" +
                "  \"Additional permissions\" are terms that supplement the terms\n" +
                "of this License by making exceptions from one or more of its\n" +
                "conditions. Additional permissions that are applicable to the\n" +
                "entire Program shall be treated as though they were included in\n" +
                "this License, to the extent that they are valid under applicable\n" +
                "law.  If additional permissions apply only to part of the\n" +
                "Program, that part may be used separately under those\n" +
                "permissions, but the entire Program remains governed by this\n" +
                "License without regard to the additional permissions.\n" +
                "\n" +
                "  When you convey a copy of a covered work, you may at your\n" +
                "option remove any additional permissions from that copy, or\n" +
                "from any part of it.  (Additional permissions may be written to\n" +
                "require their own removal in certain cases when you modify the\n" +
                "work.)  You may place additional permissions on material,\n" +
                "added by you to a covered work, for which you have or can give\n" +
                "appropriate copyright permission.\n" +
                "\n" +
                "  Notwithstanding any other provision of this License, for\n" +
                "material you add to a covered work, you may (if authorized by\n" +
                "the copyright holders of that material) supplement the terms of\n" +
                "this License with terms:\n" +
                "\n" +
                "    a) Disclaiming warranty or limiting liability differently\n" +
                "    from the terms of sections 15 and 16 of this License; or\n" +
                "\n" +
                "    b) Requiring preservation of specified reasonable legal\n" +
                "    notices or author attributions in that material or in the\n" +
                "    Appropriate Legal Notices displayed by works containing it;\n" +
                "    or\n" +
                "\n" +
                "    c) Prohibiting misrepresentation of the origin of that\n" +
                "    material, or requiring that modified versions of such\n" +
                "    material be marked in reasonable ways as different from the\n" +
                "    original version; or\n" +
                "\n" +
                "    d) Limiting the use for publicity purposes of names of\n" +
                "    licensors or authors of the material; or\n" +
                "\n" +
                "    e) Declining to grant rights under trademark law for use of\n" +
                "    some trade names, trademarks, or service marks; or\n" +
                "\n" +
                "    f) Requiring indemnification of licensors and authors of\n" +
                "    that material by anyone who conveys the material (or\n" +
                "    modified versions of it) with contractual assumptions of\n" +
                "    liability to the recipient, for any liability that these\n" +
                "    contractual assumptions directly impose on those licensors\n" +
                "    and authors.\n" +
                "\n" +
                "  All other non-permissive additional terms are considered\n" +
                "\"further restrictions\" within the meaning of section 10.  If\n" +
                "the Program as you received it, or any part of it, contains a\n" +
                "notice stating that it is governed by this License along with a\n" +
                "term that is a further restriction, you may remove that term.\n" +
                "  If a license document contains a further restriction but\n" +
                "permits relicensing or conveying under this License, you may\n" +
                "add to a covered work material governed by the terms of that\n" +
                "license document, provided that the further restriction does\n" +
                "not survive such relicensing or conveying.\n" +
                "\n" +
                "  If you add terms to a covered work in accord with this\n" +
                "section, you must place, in the relevant source files, a\n" +
                "statement of the additional terms that apply to those files,\n" +
                "or a notice indicating where to find the applicable terms.\n" +
                "\n" +
                "  Additional terms, permissive or non-permissive, may be stated\n" +
                "in the form of a separately written license, or stated as\n" +
                "exceptions; the above requirements apply either way.\n" +
                "\n" +
                "  8. Termination.\n" +
                "\n" +
                "  You may not propagate or modify a covered work except as\n" +
                "expressly provided under this License.  Any attempt otherwise\n" +
                "to propagate or modify it is void, and will automatically\n" +
                "terminate your rights under this License (including any patent\n" +
                "licenses granted under the third paragraph of section 11).\n" +
                "\n" +
                "  However, if you cease all violation of this License, then your\n" +
                "license from a particular copyright holder is reinstated (a)\n" +
                "provisionally, unless and until the copyright holder explicitly\n" +
                "and finally terminates your license, and (b) permanently, if\n" +
                "the copyright holder fails to notify you of the violation by\n" +
                "some reasonable means prior to 60 days after the cessation.\n" +
                "\n" +
                "  Moreover, your license from a particular copyright holder is\n" +
                "reinstated permanently if the copyright holder notifies you of\n" +
                "the violation by some reasonable means, this is the first time\n" +
                "you have received notice of violation of this License (for any\n" +
                "work) from that copyright holder, and you cure the violation\n" +
                "prior to 30 days after your receipt of the notice.\n" +
                "\n" +
                "  Termination of your rights under this section does not\n" +
                "terminate the licenses of parties who have received copies or\n" +
                "rights from you under this License.  If your rights have been\n" +
                "terminated and not permanently reinstated, you do not qualify\n" +
                "to receive new licenses for the same material under section 10.\n" +
                "\n" +
                "  9. Acceptance Not Required for Having Copies.\n" +
                "\n" +
                "  You are not required to accept this License in order to\n" +
                "receive or run a copy of the Program.  Ancillary propagation of\n" +
                "a covered work occurring solely as a consequence of using\n" +
                "peer-to-peer transmission to receive a copy likewise does not\n" +
                "require acceptance.  However, nothing other than this License\n" +
                "grants you permission to propagate or modify any covered work.\n" +
                "  These actions infringe copyright if you do not accept this\n" +
                "License.  Therefore, by modifying or propagating a covered\n" +
                "work, you indicate your acceptance of this License to do so.\n" +
                "\n" +
                "  10. Automatic Licensing of Downstream Recipients.\n" +
                "\n" +
                "  Each time you convey a covered work, the recipient\n" +
                "automatically receives a license from the original licensors,\n" +
                "to run, modify and propagate that work, subject to this\n" +
                "License.  You are not responsible for enforcing compliance by\n" +
                "third parties with this License.\n" +
                "\n" +
                "  An \"entity transaction\" is a transaction transferring control\n" +
                "of an organization, or substantially all assets of one, or\n" +
                "subdividing an organization, or merging organizations.  If\n" +
                "propagation of a covered work results from an entity\n" +
                "transaction, each party to that transaction who receives a\n" +
                "copy of the work also receives whatever licenses to the work\n" +
                "the party's predecessor in interest had or could give under\n" +
                "the previous paragraph, plus a right to possession of the\n" +
                "Corresponding Source of the work from the predecessor in\n" +
                "interest, if the predecessor has it or can get it with\n" +
                "reasonable efforts.\n" +
                "\n" +
                "  You may not impose any further restrictions on the exercise of\n" +
                "the rights granted or affirmed under this License.  For\n" +
                "example, you may not impose a license fee, royalty, or other\n" +
                "charge for exercise of rights granted under this License, and\n" +
                "you may not initiate litigation (including a cross-claim or\n" +
                "counterclaim in a lawsuit) alleging that any patent claim is\n" +
                "infringed by making, using, selling, offering for sale, or\n" +
                "importing the Program or any portion of it.\n" +
                "\n" +
                "  11. Patents.\n" +
                "\n" +
                "  A \"contributor\" is a copyright holder who authorizes use under\n" +
                "this License of the Program or a work on which the Program is\n" +
                "based.  The work thus licensed is called the contributor's\n" +
                "\"contributor version\".\n" +
                "\n" +
                "  A contributor's \"essential patent claims\" are all patent\n" +
                "claims owned or controlled by the contributor, whether already\n" +
                "acquired or hereafter acquired, that would be infringed by some\n" +
                "manner, permitted by this License, of making, using, or selling\n" +
                "its contributor version, but do not include claims that would\n" +
                "be infringed only as a consequence of further modification of\n" +
                "the contributor version.  For purposes of this definition,\n" +
                "\"control\" includes the right to grant patent sublicenses in a\n" +
                "manner consistent with the requirements of this License.\n" +
                "\n" +
                "  Each contributor grants you a non-exclusive, worldwide,\n" +
                "royalty-free patent license under the contributor's essential\n" +
                "patent claims, to make, use, sell, offer for sale, import and\n" +
                "otherwise run, modify and propagate the contents of its\n" +
                "contributor version.\n" +
                "\n" +
                "  In the following three paragraphs, a \"patent license\" is any\n" +
                "express agreement or commitment, however denominated, not to\n" +
                "enforce a patent (such as an express permission to practice a\n" +
                "patent or covenant not to sue for patent infringement).  To\n" +
                "\"grant\" such a patent license to a party means to make such an\n" +
                "agreement or commitment not to enforce a patent against the\n" +
                "party.\n" +
                "\n" +
                "  If you convey a covered work, knowingly relying on a patent\n" +
                "license, and the Corresponding Source of the work is not\n" +
                "available for anyone to copy, free of charge and under the terms\n" +
                "of this License, through a publicly available network server or\n" +
                "other readily accessible means, then you must either (1) cause\n" +
                "the Corresponding Source to be so available, or (2) arrange to\n" +
                "deprive yourself of the benefit of the patent license for this\n" +
                "particular work, or (3) arrange, in a manner consistent with the\n" +
                "requirements of this License, to extend the patent license to\n" +
                "downstream recipients.  \"Knowingly relying\" means you have\n" +
                "actual knowledge that, but for the patent license, your\n" +
                "conveying the covered work in a country, or your recipient's use\n" +
                "of the covered work in a country, would infringe one or more\n" +
                "identifiable patents in that country that you have reason to\n" +
                "believe are valid.\n" +
                "\n" +
                "  If, pursuant to or in connection with a single transaction or\n" +
                "arrangement, you convey, or propagate by procuring conveyance\n" +
                "of, a covered work, and grant a patent license to some of the\n" +
                "parties receiving the covered work authorizing them to use,\n" +
                "propagate, modify or convey a specific copy of the covered work,\n" +
                "then the patent license you grant is automatically extended to\n" +
                "all recipients of the covered work and works based on it.\n" +
                "\n" +
                "  A patent license is \"discriminatory\" if it does not include\n" +
                "within the scope of its coverage, prohibits the exercise of, or\n" +
                "is conditioned on the non-exercise of one or more of the rights\n" +
                "that are specifically granted under this License.  You may not\n" +
                "convey a covered work if you are a party to an arrangement with\n" +
                "a third party that is in the business of distributing software,\n" +
                "under which you make payment to the third party based on the\n" +
                "extent of your activity of conveying the work, and under which\n" +
                "the third party grants, to any of the parties who would receive\n" +
                "the covered work from you, a discriminatory patent license (a)\n" +
                "in connection with copies of the covered work conveyed by you\n" +
                "(or copies made from those copies), or (b) primarily for and in\n" +
                "connection with specific products or compilations that contain\n" +
                "the covered work, unless you entered into that arrangement, or\n" +
                "that patent license was granted, prior to 28 March 2007.\n" +
                "\n" +
                "  Nothing in this License shall be construed as excluding or\n" +
                "limiting any implied license or other defenses to infringement\n" +
                "that may otherwise be available to you under applicable patent\n" +
                "law.\n" +
                "\n" +
                "  12. No Surrender of Others' Freedom.\n" +
                "\n" +
                "  If conditions are imposed on you (whether by court order,\n" +
                "agreement or otherwise) that contradict the conditions of this\n" +
                "License, they do not excuse you from the conditions of this\n" +
                "License.  If you cannot convey a covered work so as to satisfy\n" +
                "simultaneously your obligations under this License and any other\n" +
                "pertinent obligations, then as a consequence you may not convey\n" +
                "it at all.  For example, if you agree to terms that obligate you\n" +
                "to collect a royalty for further conveying from those to whom\n" +
                "you convey the Program, the only way you could satisfy both\n" +
                "those terms and this License would be to refrain entirely from\n" +
                "conveying the Program.\n" +
                "\n" +
                "  13. Use with the GNU Affero General Public License.\n" +
                "\n" +
                "  Notwithstanding any other provision of this License, you have\n" +
                "permission to link or combine any covered work with a work\n" +
                "licensed under version 3 of the GNU Affero General Public\n" +
                "License into a single combined work, and to convey the resulting\n" +
                "work.  The terms of this License will continue to apply to the\n" +
                "part which is the covered work, but the special requirements of\n" +
                "the GNU Affero General Public License, section 13, concerning\n" +
                "interaction through a network will apply to the combination as\n" +
                "such.\n" +
                "\n" +
                "  14. Revised Versions of this License.\n" +
                "\n" +
                "  The Free Software Foundation may publish revised and/or new\n" +
                "versions of the GNU General Public License from time to time.\n" +
                "  Such new versions will be similar in spirit to the present\n" +
                "version, but may differ in detail to address new problems or\n" +
                "concerns.\n" +
                "\n" +
                "  Each version is given a distinguishing version number.  If the\n" +
                "Program specifies that a certain numbered version of the GNU\n" +
                "General Public License \"or any later version\" applies to it,\n" +
                "you have the option of following the terms and conditions either\n" +
                "of that numbered version or of any later version published by the\n" +
                "Free Software Foundation.  If the Program does not specify a\n" +
                "version number of the GNU General Public License, you may choose\n" +
                "any version ever published by the Free Software Foundation.\n" +
                "\n" +
                "  If the Program specifies that a proxy can decide which future\n" +
                "versions of the GNU General Public License can be used, that\n" +
                "proxy's public statement of acceptance of a version permanently\n" +
                "authorizes you to choose that version for the Program.\n" +
                "\n" +
                "  Later license versions may give you additional or different\n" +
                "permissions.  However, no additional obligations are imposed on\n" +
                "any author or copyright holder as a result of your choosing to\n" +
                "follow a later version.\n" +
                "\n" +
                "  15. Disclaimer of Warranty.\n" +
                "\n" +
                "  THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED\n" +
                "BY APPLICABLE LAW.  EXCEPT WHEN OTHERWISE STATED IN WRITING THE\n" +
                "COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM \"AS\n" +
                "IS\" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED,\n" +
                "INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF\n" +
                "MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  THE\n" +
                "ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS\n" +
                "WITH YOU.  SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE\n" +
                "COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.\n" +
                "\n" +
                "  16. Limitation of Liability.\n" +
                "\n" +
                "  IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN\n" +
                "WRITING WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO\n" +
                "MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE, BE\n" +
                "LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL,\n" +
                "INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR\n" +
                "INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS\n" +
                "OF DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED\n" +
                "BY YOU OR THIRD PARTIES OR A FAILURE OF THE PROGRAM TO OPERATE\n" +
                "WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS\n" +
                "BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.\n" +
                "\n" +
                "  17. Interpretation of Sections 15 and 16.\n" +
                "\n" +
                "  If the disclaimer of warranty and limitation of liability\n" +
                "provided above cannot be given local legal effect according to\n" +
                "their terms, reviewing courts shall apply local law that most\n" +
                "closely approximates an absolute waiver of all civil liability\n" +
                "in connection with the Program, unless a warranty or assumption\n" +
                "of liability accompanies a copy of the Program in return for a\n" +
                "fee.\n" +
                "\n" +
                "                     END OF TERMS AND CONDITIONS\n" +
                "\n" +
                "            How to Apply These Terms to Your New Programs\n" +
                "\n" +
                "  If you develop a new program, and you want it to be of the\n" +
                "greatest possible use to the public, the best way to achieve\n" +
                "this is to make it free software which everyone can redistribute\n" +
                "and change under these terms.\n" +
                "\n" +
                "  To do so, attach the following notices to the program.  It is\n" +
                "safest to attach them to the start of each source file to most\n" +
                "effectively state the exclusion of warranty; and each file\n" +
                "should have at least the \"copyright\" line and a pointer to\n" +
                "where the full notice is found.\n" +
                "\n" +
                "    <one line to give the program's name and a brief idea of what\n" +
                "    it does.>\n" +
                "    Copyright (C) <year>  <name of author>\n" +
                "\n" +
                "    This program is free software: you can redistribute it and/or\n" +
                "    modify it under the terms of the GNU General Public License\n" +
                "    as published by the Free Software Foundation, either version\n" +
                "    3 of the License, or (at your option) any later version.\n" +
                "\n" +
                "    This program is distributed in the hope that it will be\n" +
                "    useful, but WITHOUT ANY WARRANTY; without even the implied\n" +
                "    warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR\n" +
                "    PURPOSE.  See the GNU General Public License for more\n" +
                "    details.\n" +
                "\n" +
                "    You should have received a copy of the GNU General Public\n" +
                "    License along with this program.  If not, see\n" +
                "    <https://www.gnu.org/licenses/>.\n" +
                "\n" +
                "Also add information on how to contact you by electronic and\n" +
                "paper mail.\n" +
                "\n" +
                "  If the program does terminal interaction, make it output a\n" +
                "short notice like this when it starts in an interactive mode:\n" +
                "\n" +
                "    <program>  Copyright (C) <year>  <name of author>\n" +
                "    This program comes with ABSOLUTELY NO WARRANTY; for details\n" +
                "    type `show w'.\n" +
                "    This is free software, and you are welcome to redistribute it\n" +
                "    under certain conditions; type `show c' for details.\n" +
                "\n" +
                "The hypothetical commands `show w' and `show c' should show the\n" +
                "appropriate parts of the General Public License.  Of course,\n" +
                "your program's commands might be different; for a GUI interface,\n" +
                "you would use an \"about box\".\n" +
                "\n" +
                "  You should also get your employer (if you work as a programmer)\n" +
                "or school, if any, to sign a \"copyright disclaimer\" for the\n" +
                "program, if necessary.\n" +
                "For more information on this, and how to apply and follow the GNU\n" +
                "GPL, see <https://www.gnu.org/licenses/>.\n" +
                "\n" +
                "  The GNU General Public License does not permit incorporating\n" +
                "your program into proprietary programs.  If your program is a\n" +
                "subroutine library, you may consider it more useful to permit\n" +
                "linking proprietary applications with the library.  If this is\n" +
                "what you want to do, use the GNU Lesser General Public License\n" +
                "instead of this License.  But first, please read\n" +
                "<https://www.gnu.org/licenses/why-not-lgpl.html>.";
            String htmlContent = "<html><body style='font-family: Arial, sans-serif; font-size: 12pt; color: #333;'>" +
                "<h2>About Daily Checklist</h2>" +
                "<p>Daily Checklist is a simple application designed to help you manage your daily tasks. " +
                "It organizes tasks into morning and evening routines, with support for weekday-specific tasks to keep you on track.</p>" +
                "<h3>Version & Copyright</h3>" +
                "<p><b>Version:</b> 0.1</p>" +
                "<p><b>Copyright:</b> (C) 2025 Johan Andersson</p>" +
                "<p>This program is free software: you can redistribute it and/or modify<br>" +
                "it under the terms of the GNU General Public License as published by<br>" +
                "the Free Software Foundation, either version 3 of the License, or<br>" +
                "(at your option) any later version.</p>" +
                "<p>This program is distributed in the hope that it will be useful,<br>" +
                "but WITHOUT ANY WARRANTY; without even the implied warranty of<br>" +
                "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the<br>" +
                "GNU General Public License for more details.</p>" +
                "<p>You should have received a copy of the GNU General Public License<br>" +
                "along with this program. If not, see <a href='https://www.gnu.org/licenses/'>https://www.gnu.org/licenses/</a>.</p>" +
                "<h3>Full GNU General Public License v3</h3>" +
                "<pre style='font-size: 12pt; white-space: pre-wrap;'>" + gpl3Text + "</pre>" +
                "</body></html>";
            JEditorPane contentPane = new JEditorPane("text/html", htmlContent);
            contentPane.setEditable(false);
            contentPane.setBackground(Color.WHITE);
            contentPane.setBorder(BorderFactory.createEmptyBorder());
            contentPane.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        try {
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (Exception ex) {
                            // Handle exception, perhaps show a message
                            JOptionPane.showMessageDialog(aboutDialog, "Unable to open link: " + e.getURL(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });
            JScrollPane scrollPane = new JScrollPane(contentPane);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            contentPanel.add(scrollPane, BorderLayout.CENTER);
            aboutDialog.add(contentPanel, BorderLayout.CENTER);

            // Button panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            buttonPanel.setBackground(Color.WHITE);
            JButton closeButton = new JButton("Close");
            closeButton.setFont(new Font("Arial", Font.PLAIN, 14));
            closeButton.setBackground(Color.LIGHT_GRAY);
            closeButton.setForeground(Color.BLACK);
            closeButton.setFocusPainted(false);
            closeButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            closeButton.addActionListener(ae -> aboutDialog.dispose());
            buttonPanel.add(closeButton);
            aboutDialog.add(buttonPanel, BorderLayout.SOUTH);

            aboutDialog.setVisible(true);
        });
        fileMenu.add(aboutItem);

        // Add Help menu item
        JMenuItem helpItem = new JMenuItem("Help");
        helpItem.addActionListener(e -> {
            JDialog helpDialog = new JDialog((java.awt.Frame) parent, "Help - Daily Checklist", true);
            helpDialog.setLayout(new BorderLayout());
            helpDialog.setSize(600, 500);
            helpDialog.setLocationRelativeTo(parent);
            helpDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            // Title panel
            JPanel titlePanel = new JPanel();
            titlePanel.setBackground(new Color(0, 123, 255)); // Bootstrap blue
            JLabel titleLabel = new JLabel("Daily Checklist Help");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
            titleLabel.setForeground(Color.WHITE);
            titlePanel.add(titleLabel);
            helpDialog.add(titlePanel, BorderLayout.NORTH);

            // Content panel
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            contentPanel.setBackground(Color.WHITE);

            // Read help text from file
            String helpText = "";
            try (java.io.InputStream is = MenuBarBuilder.class.getResourceAsStream("/help.txt");
                 java.util.Scanner scanner = new java.util.Scanner(is, "UTF-8")) {
                helpText = scanner.useDelimiter("\\A").next();
            } catch (Exception ex) {
                helpText = "Help file not found. Please refer to the README.md file for detailed instructions.";
            }

            JTextPane contentPane = new JTextPane();
            contentPane.setEditable(false);
            contentPane.setBackground(Color.WHITE);
            contentPane.setBorder(BorderFactory.createEmptyBorder());

            // Set up the styled document
            StyledDocument doc = contentPane.getStyledDocument();

            // Create styles
            Style defaultStyle = doc.addStyle("default", null);
            StyleConstants.setFontFamily(defaultStyle, "Arial");
            StyleConstants.setFontSize(defaultStyle, 12);

            Style header1Style = doc.addStyle("h1", defaultStyle);
            StyleConstants.setFontSize(header1Style, 24);
            StyleConstants.setBold(header1Style, true);

            Style header2Style = doc.addStyle("h2", defaultStyle);
            StyleConstants.setFontSize(header2Style, 18);
            StyleConstants.setBold(header2Style, true);

            Style header3Style = doc.addStyle("h3", defaultStyle);
            StyleConstants.setFontSize(header3Style, 14);
            StyleConstants.setBold(header3Style, true);

            Style boldStyle = doc.addStyle("bold", defaultStyle);
            StyleConstants.setBold(boldStyle, true);

            // Parse and insert HTML-like content with icons
            insertStyledTextWithIcons(doc, helpText, defaultStyle, boldStyle);

            JScrollPane scrollPane = new JScrollPane(contentPane);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            contentPanel.add(scrollPane, BorderLayout.CENTER);
            helpDialog.add(contentPanel, BorderLayout.CENTER);

            // Button panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            buttonPanel.setBackground(Color.WHITE);
            JButton closeButton = new JButton("Close");
            closeButton.setFont(new Font("Arial", Font.PLAIN, 14));
            closeButton.setBackground(Color.LIGHT_GRAY);
            closeButton.setForeground(Color.BLACK);
            closeButton.setFocusPainted(false);
            closeButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            closeButton.addActionListener(ae -> helpDialog.dispose());
            buttonPanel.add(closeButton);
            helpDialog.add(buttonPanel, BorderLayout.SOUTH);

            helpDialog.setVisible(true);
        });
        fileMenu.add(helpItem);

        //Add a refresh option that reloads tasks from repository
        JMenuItem refreshItem = new JMenuItem("Refresh Tasks");
        refreshItem.addActionListener(e -> updateTasks.run());
        fileMenu.add(refreshItem);

        //Add restore from backup option
        JMenuItem restoreItem = new JMenuItem("Restore from Backup");
        restoreItem.addActionListener(e -> {
            BackupRestoreDialog.showRestoreDialog(parent, taskManager, updateTasks);
        });
        fileMenu.add(restoreItem);

        menuBar.add(fileMenu);
        return menuBar;
    }

    private String readGpl3Text() {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/gpl3.txt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (Exception e) {
            // Fallback to a short message if file not found
            return "GNU General Public License v3\n\nSee https://www.gnu.org/licenses/gpl-3.0.html for the full text.";
        }
        return sb.toString();
    }

    private static void insertStyledTextWithIcons(StyledDocument doc, String htmlText, Style defaultStyle, Style boldStyle) {
        try {
            // Remove HTML tags and process content
            String text = htmlText.replaceAll("<[^>]+>", "").trim();

            // Split by lines and process each line
            String[] lines = text.split("\n");
            boolean inList = false;

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) {
                    doc.insertString(doc.getLength(), "\n", defaultStyle);
                    continue;
                }

                if (line.startsWith("Daily Checklist Help")) {
                    Style h1Style = doc.addStyle("h1", null);
                    StyleConstants.setFontFamily(h1Style, "Arial");
                    StyleConstants.setFontSize(h1Style, 24);
                    StyleConstants.setBold(h1Style, true);
                    doc.insertString(doc.getLength(), line + "\n\n", h1Style);
                } else if (line.startsWith("Getting Started") || line.startsWith("Managing Daily Tasks") ||
                          line.startsWith("Managing Custom Checklists") || line.startsWith("Reminders") ||
                          line.startsWith("Focus Timer") || line.startsWith("Reminder Icon Guide")) {
                    Style h2Style = doc.addStyle("h2", null);
                    StyleConstants.setFontFamily(h2Style, "Arial");
                    StyleConstants.setFontSize(h2Style, 18);
                    StyleConstants.setBold(h2Style, true);
                    doc.insertString(doc.getLength(), line + "\n\n", h2Style);
                } else if (line.startsWith("Adding a New Daily Task") || line.startsWith("Viewing Daily Tasks") ||
                          line.startsWith("Editing Daily Tasks") || line.startsWith("Creating Custom Checklists") ||
                          line.startsWith("Managing Custom Checklists") || line.startsWith("Setting Reminders") ||
                          line.startsWith("Using the Focus Timer")) {
                    Style h3Style = doc.addStyle("h3", null);
                    StyleConstants.setFontFamily(h3Style, "Arial");
                    StyleConstants.setFontSize(h3Style, 14);
                    StyleConstants.setBold(h3Style, true);
                    doc.insertString(doc.getLength(), line + "\n", h3Style);
                } else if (line.contains("[RED_CLOCK_ICON]") || line.contains("[YELLOW_CLOCK_ICON]") || line.contains("[BLUE_CLOCK_ICON]")) {
                    // Handle icon lines
                    String processedLine = line;
                    if (line.contains("[RED_CLOCK_ICON]")) {
                        insertIconAndText(doc, processedLine.replace("[RED_CLOCK_ICON]", ""), new ReminderClockIcon(9, 30, ReminderClockIcon.State.OVERDUE), boldStyle);
                    } else if (line.contains("[YELLOW_CLOCK_ICON]")) {
                        insertIconAndText(doc, processedLine.replace("[YELLOW_CLOCK_ICON]", ""), new ReminderClockIcon(9, 30, ReminderClockIcon.State.DUE_SOON), boldStyle);
                    } else if (line.contains("[BLUE_CLOCK_ICON]")) {
                        insertIconAndText(doc, processedLine.replace("[BLUE_CLOCK_ICON]", ""), new ReminderClockIcon(9, 30, ReminderClockIcon.State.FUTURE), boldStyle);
                    }
                    doc.insertString(doc.getLength(), "\n", defaultStyle);
                } else if (line.startsWith("-") || line.startsWith("•")) {
                    // List items
                    String listItem = line.substring(1).trim();
                    if (listItem.contains("**")) {
                        // Handle bold text in list items
                        String[] parts = listItem.split("\\*\\*");
                        for (int i = 0; i < parts.length; i++) {
                            Style style = (i % 2 == 1) ? boldStyle : defaultStyle;
                            doc.insertString(doc.getLength(), parts[i], style);
                        }
                    } else {
                        doc.insertString(doc.getLength(), "• " + listItem, defaultStyle);
                    }
                    doc.insertString(doc.getLength(), "\n", defaultStyle);
                } else {
                    // Regular paragraphs
                    if (line.contains("**")) {
                        // Handle bold text
                        String[] parts = line.split("\\*\\*");
                        for (int i = 0; i < parts.length; i++) {
                            Style style = (i % 2 == 1) ? boldStyle : defaultStyle;
                            doc.insertString(doc.getLength(), parts[i], style);
                        }
                    } else {
                        doc.insertString(doc.getLength(), line, defaultStyle);
                    }
                    doc.insertString(doc.getLength(), "\n\n", defaultStyle);
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private static void insertIconAndText(StyledDocument doc, String text, ReminderClockIcon icon, Style style) throws BadLocationException {
        // Insert the icon
        Style iconStyle = doc.addStyle("icon", null);
        StyleConstants.setIcon(iconStyle, icon);
        doc.insertString(doc.getLength(), " ", iconStyle);

        // Insert the text
        doc.insertString(doc.getLength(), text, style);
    }
}