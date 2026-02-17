import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { MessengerWSProvider } from './context/MessengerWSContext';
import { Layout } from './components/Layout';
import { Home } from './pages/Home';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { ConfirmEmail } from './pages/ConfirmEmail';
import { ForgotPassword } from './pages/ForgotPassword';
import { ResetPassword } from './pages/ResetPassword';
import { Profile } from './pages/Profile';
import { SellerProfile } from './pages/SellerProfile';
import { SellerProfileView } from './pages/SellerProfileView';
import { UserProfileView } from './pages/UserProfileView';
import { Listings } from './pages/Listings';
import { ListingForm } from './pages/ListingForm';
import { ListingDetail } from './pages/ListingDetail';
import { ListingPhotos } from './pages/ListingPhotos';
import { Search } from './pages/Search';
import { Favourites } from './pages/Favourites';
import { ReviewForm } from './pages/ReviewForm';
import { Chats } from './pages/Chats';
import { Chat } from './pages/Chat';
import { RequireAuth, RequireSeller } from './components/AccessGate';

function App() {
  return (
    <AuthProvider>
      <MessengerWSProvider>
        <BrowserRouter>
          <Layout>
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route path="/confirm" element={<ConfirmEmail />} />
              <Route path="/forgot-password" element={<ForgotPassword />} />
              <Route path="/reset-password" element={<ResetPassword />} />
              <Route path="/profile" element={<RequireAuth><Profile /></RequireAuth>} />
              <Route path="/seller/profile" element={<RequireAuth><SellerProfile /></RequireAuth>} />
              <Route path="/seller/profile/view/:sellerId" element={<SellerProfileView />} />
              <Route path="/user/profile/:userId" element={<UserProfileView />} />
              <Route path="/listings" element={<RequireSeller><Listings /></RequireSeller>} />
              <Route path="/listings/create" element={<RequireSeller><ListingForm /></RequireSeller>} />
              <Route path="/listings/:id" element={<ListingDetail />} />
              <Route path="/listings/:id/edit" element={<RequireSeller><ListingForm /></RequireSeller>} />
              <Route path="/listings/:id/photos" element={<RequireSeller><ListingPhotos /></RequireSeller>} />
              <Route path="/listings/:id/review" element={<RequireAuth><ReviewForm /></RequireAuth>} />
              <Route path="/search" element={<Search />} />
              <Route path="/favourites" element={<RequireAuth><Favourites /></RequireAuth>} />
              <Route path="/chats" element={<RequireAuth><Chats /></RequireAuth>} />
              <Route path="/chats/:chatId" element={<RequireAuth><Chat /></RequireAuth>} />
            </Routes>
          </Layout>
        </BrowserRouter>
      </MessengerWSProvider>
    </AuthProvider>
  );
}

export default App;
