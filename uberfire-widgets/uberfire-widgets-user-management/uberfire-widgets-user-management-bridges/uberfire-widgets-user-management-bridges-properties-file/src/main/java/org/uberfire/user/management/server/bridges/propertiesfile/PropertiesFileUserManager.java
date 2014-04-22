/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.uberfire.user.management.server.bridges.propertiesfile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.commons.validation.PortablePreconditions;
import org.uberfire.security.auth.AuthenticationSource;
import org.uberfire.security.server.auth.source.PropertyUserSource;
import org.uberfire.security.server.cdi.AppAuthenticationStores;
import org.uberfire.user.management.service.UserManager;

@ApplicationScoped
public class PropertiesFileUserManager implements UserManager {

    private static final Logger logger = LoggerFactory.getLogger( PropertiesFileUserManager.class );

    @Inject
    @AppAuthenticationStores
    private List<AuthenticationSource> authenticationSources;

    private PropertyUserSource propertyUserSource;
    private String propertyUserSourcePath = null;

    private Properties properties = null;

    @PostConstruct
    private void initialise() {
        for ( AuthenticationSource authenticationSource : authenticationSources ) {
            if ( authenticationSource instanceof PropertyUserSource ) {
                propertyUserSource = (PropertyUserSource) authenticationSource;
                propertyUserSourcePath = (String) propertyUserSource.getOptions().get( "usersPropertyFile" );
                break;
            }
        }
        if ( propertyUserSource == null ) {
            logger.info( "org.uberfire.security.server.auth.source.PropertyUserSource not specified in META-INF/services/org.uberfire.security.auth.AuthenticationSource. User Management will be disabled." );
        }
        if ( propertyUserSourcePath == null ) {
            logger.info( "org.uberfire.security.server.UberFireSecurityFilter's usersPropertyFile init parameter has not been set. User Management will be disabled." );
        }
    }

    @Override
    public boolean isAddUserSupported() {
        return !( propertyUserSourcePath == null || propertyUserSource == null );
    }

    @Override
    public boolean isUpdateUserSupported() {
        return !( propertyUserSourcePath == null || propertyUserSource == null );
    }

    @Override
    public boolean isDeleteUserSupported() {
        return !( propertyUserSourcePath == null || propertyUserSource == null );
    }

    @Override
    public synchronized Set<String> getUserNames() {
        if ( properties == null ) {
            properties = loadProperties();
        }

        logger.info( "Retrieving User Names..." );

        //Strip out User Names
        final Set<String> userNames = new HashSet<String>();
        for ( Object e : properties.keySet() ) {
            userNames.add( e.toString() );
        }

        return userNames;
    }

    @Override
    public synchronized Set<String> getUserRoles( final String userName ) {
        PortablePreconditions.checkNotNull( "userName",
                                            userName );

        if ( properties == null ) {
            properties = loadProperties();
        }

        logger.info( "Retrieving User Roles for User Name '" + userName + "'..." );

        //Strip out roles for user
        final Set<String> userRoles = new HashSet<String>();
        final String value = properties.getProperty( userName );
        if ( !( value == null || value.isEmpty() ) ) {
            userRoles.addAll( parseUserRoles( value ) );
        }

        return userRoles;
    }

    @Override
    public synchronized void addUser( final String userName,
                                      final String userPassword,
                                      final Set<String> userRoles ) {
        PortablePreconditions.checkNotNull( "userName",
                                            userName );
        PortablePreconditions.checkNotNull( "userPassword",
                                            userPassword );
        PortablePreconditions.checkNotNull( "userRoles",
                                            userRoles );

        if ( properties == null ) {
            properties = loadProperties();
        }

        logger.info( "Adding User: '" + userName + "' with User Roles [" + dumpUserRoles( userRoles ) + "]." );

        properties.setProperty( userName,
                                makePropertiesFileValue( userPassword,
                                                         userRoles ) );
        saveProperties();
    }

    @Override
    public synchronized void updateUserPassword( final String userName,
                                                 final String userPassword ) {
        PortablePreconditions.checkNotNull( "userName",
                                            userName );
        PortablePreconditions.checkNotNull( "userPassword",
                                            userPassword );

        if ( properties == null ) {
            properties = loadProperties();
        }

        logger.info( "Updating Password for User: '" + userName + "'." );

        final Set<String> userRoles = parseUserRoles( properties.getProperty( userName ) );
        properties.setProperty( userName,
                                makePropertiesFileValue( userPassword,
                                                         userRoles ) );
        saveProperties();
    }

    @Override
    public synchronized void updateUserRoles( final String userName,
                                              final Set<String> userRoles ) {
        PortablePreconditions.checkNotNull( "userName",
                                            userName );
        PortablePreconditions.checkNotNull( "userRoles",
                                            userRoles );

        if ( properties == null ) {
            properties = loadProperties();
        }

        logger.info( "Updating Roles for User: '" + userName + "'. User Roles [" + dumpUserRoles( userRoles ) + "]." );

        final String userPassword = parseUserPassword( properties.getProperty( userName ) );
        properties.setProperty( userName,
                                makePropertiesFileValue( userPassword,
                                                         userRoles ) );
        saveProperties();
    }

    @Override
    public synchronized void addUserRole( final String userName,
                                          final String userRole ) {
        PortablePreconditions.checkNotNull( "userName",
                                            userName );
        PortablePreconditions.checkNotNull( "userRoles",
                                            userRole );

        if ( properties == null ) {
            properties = loadProperties();
        }

        logger.info( "Adding Role '" + userRole + "' to User: '" + userName + "'." );

        final String userPassword = parseUserPassword( properties.getProperty( userName ) );
        final Set<String> userRoles = parseUserRoles( properties.getProperty( userName ) );
        userRoles.add( userRole );
        properties.setProperty( userName,
                                makePropertiesFileValue( userPassword,
                                                         userRoles ) );
        saveProperties();
    }

    @Override
    public synchronized void removeUserRole( final String userName,
                                             final String userRole ) {
        PortablePreconditions.checkNotNull( "userName",
                                            userName );
        PortablePreconditions.checkNotNull( "userRoles",
                                            userRole );

        if ( properties == null ) {
            properties = loadProperties();
        }

        logger.info( "Removing Role '" + userRole + "' from User: '" + userName + "'." );

        final String userPassword = parseUserPassword( properties.getProperty( userName ) );
        final Set<String> userRoles = parseUserRoles( properties.getProperty( userName ) );
        userRoles.remove( userRole );
        properties.setProperty( userName,
                                makePropertiesFileValue( userPassword,
                                                         userRoles ) );
        saveProperties();
    }

    @Override
    public synchronized void deleteUser( final String userName ) {
        if ( properties == null ) {
            properties = loadProperties();
        }

        logger.info( "Deleting User: '" + userName + "'." );

        properties.remove( userName );
        saveProperties();
    }

    private Properties loadProperties() {
        InputStream is = null;
        try {
            if ( propertyUserSourcePath == null ) {
                throw new RuntimeException( "org.uberfire.security.server.UberFireSecurityFilter's usersPropertyFile init parameter has not been set." );
            }

            is = new FileInputStream( propertyUserSourcePath );

            if ( is == null ) {
                throw new RuntimeException( "Unable to find properties file." );
            }

            logger.info( "Loading User information from '" + propertyUserSourcePath + "'." );

            final Properties properties = new Properties();
            properties.load( is );
            return properties;

        } catch ( FileNotFoundException e ) {
            logger.error( e.getMessage(),
                          e );
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            logger.error( e.getMessage(),
                          e );
            throw new RuntimeException( e );
        } finally {
            if ( is != null ) {
                try {
                    is.close();
                } catch ( Exception e ) {
                }
            }
        }
    }

    private void saveProperties() {
        OutputStream os = null;
        try {
            if ( propertyUserSourcePath == null ) {
                throw new RuntimeException( "org.uberfire.security.server.UberFireSecurityFilter's usersPropertyFile init parameter has not been set." );
            }

            os = new FileOutputStream( propertyUserSourcePath );

            if ( os == null ) {
                throw new RuntimeException( "Unable to find properties file." );
            }

            logger.info( "Saving User information to '" + propertyUserSourcePath + "'." );

            properties.store( os,
                              "" );

        } catch ( FileNotFoundException e ) {
            logger.error( e.getMessage(),
                          e );
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            logger.error( e.getMessage(),
                          e );
            throw new RuntimeException( e );
        } finally {
            if ( os != null ) {
                try {
                    os.close();
                } catch ( Exception e ) {
                }
            }
        }
    }

    private String dumpUserRoles( final Set<String> userRoles ) {
        final StringBuffer sb = new StringBuffer();
        if ( !( userRoles == null || userRoles.isEmpty() ) ) {
            for ( String userRole : userRoles ) {
                sb.append( userRole ).append( "," );
            }
            sb.delete( sb.length() - 1,
                       sb.length() );
        }
        return sb.toString();
    }

    private String makePropertiesFileValue( final String userPassword,
                                            final Set<String> userRoles ) {
        final StringBuffer sb = new StringBuffer( userPassword );
        sb.append( "," ).append( dumpUserRoles( userRoles ) );
        return sb.toString();
    }

    private Set<String> parseUserRoles( final String value ) {
        final Set<String> userRoles = new HashSet<String>();
        if ( value == null || value.isEmpty() ) {
            return userRoles;
        }

        final String[] result = value.split( "," );
        if ( result.length > 1 ) {
            for ( int i = 1; i < result.length; i++ ) {
                final String role = result[ i ];
                userRoles.add( role );
            }
        }
        return userRoles;
    }

    private String parseUserPassword( final String value ) {
        if ( !( value == null || value.isEmpty() ) ) {
            final String[] result = value.split( "," );
            if ( result.length > 0 ) {
                return result[ 0 ];
            }
        }
        return "";
    }

}
