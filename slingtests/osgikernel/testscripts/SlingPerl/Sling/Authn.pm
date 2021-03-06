#!/usr/bin/perl

package Sling::Authn;

=head1 NAME

Authn - useful utility functions for general Authn functionality.

=head1 ABSTRACT

Utility library providing useful utility functions for general Authn functionality.

=cut

#{{{imports
use strict;
use lib qw ( .. );
use LWP::UserAgent ();
use Sling::AuthnUtil;
use Sling::Print;
use Sling::Request;
use Sling::URL;
#}}}

#{{{sub new

=pod

=head2 new

Create, set up, and return a User Agent.

=cut

sub new {
    my ( $class, $url, $username, $password, $type, $verbose, $log ) = @_;
    die "url not defined!" unless defined $url;
    $type = ( defined $type ? $type : "basic" );

    my $lwpUserAgent = LWP::UserAgent->new( keep_alive=>1 );
    push @{ $lwpUserAgent->requests_redirectable }, 'POST';
    $lwpUserAgent->cookie_jar( { file => "/tmp/UserAgentCookies$$.txt" });

    my $response;
    my $authn = { BaseURL => "$url",
                  LWP => \$lwpUserAgent,
                  Type => $type,
                  Username => $username,
                  Password => $password,
                  Message => "",
		  Response => \$response,
		  Verbose => $verbose,
		  Log => $log };
    # Authn references itself to be compatibile with Sling::Request::request
    $authn->{ 'Authn' } = \$authn;
    bless( $authn, $class );

    # Apply basic authentication to the user agent if url, username and
    # password are supplied:
    if ( defined $url && defined $username && defined $password ) {
	if ( $type =~ /^basic$/ ) {
	    my $success = $authn->basic_login();
	    if ( ! $success ) {
	        die "Basic Auth log in for user \"$username\" at URL \"$url\" was unsuccessful\n";
	    }
        }
	elsif ( $type =~ /^form$/ ) {
	    my $success = $authn->form_login();
	    if ( ! $success ) {
	        die "Form log in for user \"$username\" at URL \"$url\" was unsuccessful\n";
	    }
	}
	else {
	    die "Unsupported auth type: \"" . $type . "\"\n"; 
	}
        if ( $verbose >= 1 ) {
            Sling::Print::print_result( $authn );
	}
    }
    return $authn;
}
#}}}

#{{{sub set_results
sub set_results {
    my ( $user, $message, $response ) = @_;
    $user->{ 'Message' } = $message;
    $user->{ 'Response' } = $response;
    return 1;
}
#}}}

#{{{sub basic_login
sub basic_login {
    my ( $authn ) = @_;
    my $res = Sling::Request::request( \$authn,
        Sling::AuthnUtil::basic_login_setup( $authn->{ 'BaseURL' } ) );
    my $success = Sling::AuthnUtil::basic_login_eval( $res );
    my $message = "Basic auth log in ";
    $message .= ( $success ? "succeeded!" : "failed!" );
    $authn->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub form_login
sub form_login {
    my ( $authn ) = @_;
    my $username = $authn->{ 'Username' };
    my $password = $authn->{ 'Password' };
    my $res = Sling::Request::request( \$authn,
        Sling::AuthnUtil::form_login_setup( $authn->{ 'BaseURL' }, $username, $password ) );
    my $success = Sling::AuthnUtil::form_login_eval( $res );
    my $message = "Form log in as user \"$username\" ";
    $message .= ( $success ? "succeeded!" : "failed!" );
    $authn->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub form_logout
sub form_logout {
    my ( $authn ) = @_;
    my $res = Sling::Request::request( \$authn,
        Sling::AuthnUtil::form_logout_setup( $authn->{ 'BaseURL' } ) );
    my $success = Sling::AuthnUtil::form_logout_eval( $res );
    my $message = "Form log out ";
    $message .= ( $success ? "succeeded!" : "failed!" );
    $authn->set_results( "$message", $res );
    return $success;
}
#}}}

#{{{sub switch_user 
sub switch_user {
    my ( $authn, $new_username, $new_password, $type, $check_basic ) = @_;
    die "New username to switch to not defined" unless defined $new_username;
    die "New password to use in switch not defined" unless defined $new_password;
    if ( ( $authn->{ 'Username' } !~ /^$new_username$/ ) || ( $authn->{ 'Password' } !~ /^$new_password$/ ) ) {
        $authn->{ 'Username' } = $new_username;
        $authn->{ 'Password' } = $new_password;
        if ( $authn->{ 'Type' } =~ /^form$/ ) {
	    # If we were previously using form auth then we must log
	    # out with form auth, even if we are switching to basic auth.
	    my $success = $authn->form_logout();
	    if ( ! $success ) {
	        die "Form Auth log out for user \"". $authn->{ 'Username' } .
		    "\" at URL \"" . $authn->{ 'BaseURL' } . "\" was unsuccessful\n";
	    }
	}
        if ( defined $type ) {
            $authn->{ 'Type' } = $type;
        }
        $check_basic = ( defined $check_basic ? $check_basic : 0 );
        if ( $authn ->{ 'Type' } =~ /^basic$/ ) {
            if ( $check_basic ) {
	        my $success = $authn->basic_login();
	        if ( ! $success ) {
	            die "Basic Auth log in for user \"$new_username\" at URL \"" .
		        $authn->{ 'BaseURL' } . "\" was unsuccessful\n";
	        }
	    }
	    else {
	        $authn->{ 'Message' } = "Fast User Switch completed!";
	    }
        }
        elsif ( $authn ->{ 'Type' } =~ /^form$/ ) {
	    my $success = $authn->form_login();
	    if ( ! $success ) {
	        die "Form Auth log in for user \"$new_username\" at URL \"" .
	            $authn->{ 'BaseURL' } . "\" was unsuccessful\n";
	    }
        }
        else {
            die "Unsupported auth type: \"" . $type . "\"\n"; 
        }
    }
    else {
        $authn->{ 'Message' } = "User already active, no need to switch!";
    }
    if ( $authn->{ 'Verbose' } >= 1 ) {
        Sling::Print::print_result( $authn );
    }
    return 1;
}
#}}}

1;
