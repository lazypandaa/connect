import React, { useState } from 'react';
import './../assets/css/LoginPage.css';
import leftImage from './../assets/images/connekta+IconLogoTagline.png';
import connecta from './../assets/images/connekta logo.png';
import { Link, useNavigate } from 'react-router-dom';
import axios from "axios";

const LoginPage = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      // Log the exact data being sent
      const loginData = {
        email: formData.email.trim(),
        password: formData.password
      };
      console.log('Sending login request with data:', loginData);
      
      const response = await axios.post(
        'http://localhost:8080/api/auth/login',
        loginData,
        {
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          },
          withCredentials: true
        }
      );

      console.log('Login response:', response.data);

      if (response.data && response.data.token) {
        // Store token and user data
        localStorage.setItem('token', response.data.token);
        localStorage.setItem('user', JSON.stringify(response.data.user));

        // Set the token in axios default headers for future requests
        axios.defaults.headers.common['Authorization'] = `Bearer ${response.data.token}`;

        // Redirect to dashboard or home page
        navigate('/');
      } else {
        console.error('Invalid response format:', response.data);
        setError('Invalid response from server');
      }
    } catch (error) {
      console.error('Login error:', error);
      if (error.response) {
        // Server responded with an error
        console.error('Error response data:', error.response.data);
        console.error('Error response status:', error.response.status);
        console.error('Error response headers:', error.response.headers);
        
        // Try to get a more specific error message
        const errorMessage = error.response.data?.message || 
                           error.response.data?.error || 
                           error.response.data || 
                           'Invalid email or password';
        setError(errorMessage);
      } else if (error.request) {
        // Request was made but no response received
        console.error('No response received:', error.request);
        setError('No response from server. Please check if the server is running.');
      } else {
        // Error in request setup
        console.error('Request setup error:', error.message);
        setError('An error occurred. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="left-bar">
        <img src={leftImage} alt="Connekta" />
      </div>
      <div className="login-container">
        <div className="flex justify-center items-center">
          <img className="w-10 h-10" src={connecta} alt="Connekta" />
        </div>
        {error && (
          <div className="error-message">
            {error}
          </div>
        )}
        <form onSubmit={handleLogin}>
          <input
            type="email"
            name="email"
            placeholder="Email"
            value={formData.email}
            onChange={handleChange}
            id="email"
            required
          />
          <input
            type="password"
            name="password"
            placeholder="Password"
            value={formData.password}
            onChange={handleChange}
            id="password"
            required
          />
          <div className="button-group">
            <button type="submit" disabled={loading}>
              {loading ? 'Signing in...' : 'Login'}
            </button>
            <button className="signup-button">
              <Link to="/signup">Sign Up</Link>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default LoginPage; 