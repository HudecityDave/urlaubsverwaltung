package org.synyx.urlaubsverwaltung.core.settings;

import javax.persistence.Column;
import javax.persistence.Embeddable;


/**
 * Mail relevant settings.
 *
 * @author  Aljona Murygina - murygina@synyx.de
 */
@Embeddable
public class MailSettings {

    @Column(name = "mail_active")
    private boolean active = false;

    @Column(name = "mail_host")
    private String host = "localhost";

    @Column(name = "mail_port")
    private Integer port = 25;

    @Column(name = "mail_username")
    private String username;

    @Column(name = "mail_password")
    private String password;

    @Column(name = "mail_from")
    private String from = "absender@uv.de";

    @Column(name = "mail_administrator")
    private String administrator = "admin@uv.de";

    public boolean isActive() {

        return active;
    }


    public void setActive(boolean active) {

        this.active = active;
    }


    public String getHost() {

        return host;
    }


    public void setHost(String host) {

        this.host = host;
    }


    public Integer getPort() {

        return port;
    }


    public void setPort(Integer port) {

        this.port = port;
    }


    public String getUsername() {

        return username;
    }


    public void setUsername(String username) {

        this.username = username;
    }


    public String getPassword() {

        return password;
    }


    public void setPassword(String password) {

        this.password = password;
    }


    public String getFrom() {

        return from;
    }


    public void setFrom(String from) {

        this.from = from;
    }


    public String getAdministrator() {

        return administrator;
    }


    public void setAdministrator(String administrator) {

        this.administrator = administrator;
    }
}
